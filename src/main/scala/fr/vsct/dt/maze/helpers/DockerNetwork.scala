/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.vsct.dt.maze.helpers

import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.StrictLogging
import fr.vsct.dt.maze.topology.{ClusterNodeGroup, Docker, DockerClusterNode, NodeGroup}

import collection.JavaConverters._
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object DockerNetwork extends StrictLogging {

  def networkName: String = Docker.extraConfiguration.networkName
  def networkRange: String = Docker.extraConfiguration.networkIpRange
  def subnet: String = s"$networkRange.0/24"

  var nodesWithIpTablesModified: Seq[DockerClusterNode] = Seq()

  private val counter = new AtomicInteger()
  private var reservedIps: Set[String] = Set[String]()

  def takeIp(): String = {
    @tailrec
    def nextIp(): String = {
      val lastIpPart: Int = counter.getAndIncrement() % 252 + 2
      val ip: String = s"$networkRange.$lastIpPart"
      if(!reservedIps.contains(ip)){
        ip
      } else {
        nextIp()
      }
    }
    if(reservedIps.size == 253) {
      throw new IllegalStateException("Cannot register more than 253 containers on the network.")
    }
    val ip: String = nextIp()
    reservedIps += ip
    ip
  }

  def freeIp(ip: String): Unit = {
    reservedIps = reservedIps.filter(_ != ip)
  }

  def createDefaultNetwork(): Unit = {
    if (exists()) {
      removeDefaultNetwork()
    }
    if (!exists()) {
      Docker.createNetwork(networkName, subnet)
    }
  }

  private def exists() = {
    Docker.client.listNetworksCmd().withNameFilter(networkName).exec().asScala.nonEmpty
  }

  def removeDefaultNetwork(): Unit = {
    Try(Docker.deleteNetwork(networkName))
  }

  private val acceptAllRules = "iptables -F && iptables -P INPUT ACCEPT && iptables -P OUTPUT ACCEPT && iptables -P FORWARD ACCEPT"
  private def dropAllRules = s"iptables -A INPUT -d $subnet -j DROP && iptables -A OUTPUT -d $subnet -j DROP && iptables -A FORWARD -d $subnet -j DROP"
  private def acceptGatewayRules =
    s"iptables -A INPUT -d $networkRange.1 -j ACCEPT && iptables -A OUTPUT -d $networkRange.1 -j ACCEPT && iptables -A FORWARD -d $networkRange.1 -j ACCEPT"
  private val saveIpTablesRules = "/sbin/service iptables save"

  /* Each Seq[DockerClusterNode] provided will not see the other ones
     Note that a node not included in such a Seq will still be able to see every other nodes
   */
  def split(partitions: NodeGroup*): Unit = {

    // This only works in privileged mode.
    // If ip is a node from another partition, in order to change the firewall rules, we want to execute :
    // iptables -F (flush the iptables rules)
    // iptables -P INPUT ACCEPT (by default, accept)
    // iptables -P FORWARD ACCEPT
    // iptables -P OUTPUT ACCEPT
    // iptables -A OUTPUT -d <ip> -j DROP (configure exceptions)
    // iptables -A INPUT -d <ip> -j DROP
    // iptables -A FORWARD -d <ip> -j DROP
    // /sbin/service iptables save

    val everything: Set[DockerClusterNode] = partitions.flatMap(_.nodes).toSet

    val ips: Map[String, String] = everything.map(node => (node.containerId, Docker.containerInfo(node.containerId)
      .getNetworkSettings.getNetworks.get(networkName).getIpAddress)).toMap

    // Reformat the data to get a map as such: Map[<container id>, Set[<invisible ip address>]]
    val unreachables: Map[String, Set[String]] =
      (
        for {
          container <- everything
          partition <- partitions.map(_.nodes) if !partition.contains(container)
          otherContainer <- partition
        } yield (container.containerId, ips(otherContainer.containerId))
        ).groupBy { case (containerId, _) => containerId }
        // groupBy will return a Map[String, Set[(String, String)]], transform it to a Map[String, Set[String]]
        .map { case (containerId, unreachableIp) => (containerId, unreachableIp.map { case (_, value) => value }) }

    unreachables.foreach { case (container, unreachableIps) =>

      val commands = unreachableIps.toSeq.map { ip =>
        s"iptables -A INPUT -d $ip -j DROP && iptables -A OUTPUT -d $ip -j DROP && iptables -A FORWARD -d $ip -j DROP"
      }.mkString(" && ")

      val command: Seq[String] = Seq("/bin/bash", "-c", s"$acceptAllRules && $acceptGatewayRules && $commands && $saveIpTablesRules")
      Docker.executionOnContainer(container, command: _*).execute()
    }

    nodesWithIpTablesModified ++= partitions.flatMap(_.nodes)
  }

  /* Each provided Seq[DockerClusterNode] will be isolated from the rest of the world
     The main difference with split is that nodes not included in such Seq will not see the isolated nodes
     Note that network traffic coming from outside the subnet wil still reach these nodes, allowing control from outside
   */
  def isolate(partitions: ClusterNodeGroup[DockerClusterNode]*): Unit = {

    // This only works in privileged mode.
    // If ip is a node from the same partition, in order to change the firewall rules, we want to execute (network is the subnet) :
    // iptables -F (flush the ip-tables rules)
    // iptables -P INPUT ACCEPT (by default, accept)
    // iptables -P FORWARD ACCEPT
    // iptables -P OUTPUT ACCEPT
    // iptables -A OUTPUT -d <ip> -j ACCEPT
    // iptables -A INPUT -d <ip> -j ACCEPT
    // iptables -A FORWARD -d <ip> -j ACCEPT
    // iptables -A INPUT -d <network> -j DROP
    // iptables -A OUTPUT -d <network> -j DROP
    // iptables -A FORWARD -d <network> -j DROP
    // /sbin/service iptables save

    val nodesAndTheirBuddiesIP: Map[DockerClusterNode, Seq[String]] = (for {
      partition <- partitions
      container <- partition.nodes
      otherContainer <- partition.nodes
    } yield (container, otherContainer.containerId)
      ).groupBy { case (container, _) => container }
      // groupBy will return a Map[String, Set[(String, String)]], transform it to a Map[String, Set[String]]
      .map {
      case (containerId, ips) =>
        (
          containerId,
          ips.map {
            case (_, id) => Docker.containerInfo(id).getNetworkSettings.getNetworks.get(networkName).getIpAddress
          }
        )
    }

    nodesAndTheirBuddiesIP.foreach { case (container, ips) =>
      val commands = ips.map { ip =>
        s"iptables -A INPUT -d $ip -j ACCEPT && iptables -A OUTPUT -d $ip -j ACCEPT && iptables -A FORWARD -d $ip -j ACCEPT"
      }.mkString(" && ")

      val command = Seq("/bin/bash", "-c", s"$acceptAllRules && $acceptGatewayRules && $commands && $dropAllRules && $saveIpTablesRules")
      logger.debug(s"Isolating node ${container.hostname} to only communicate with ${ips.mkString(", ")}")
      Docker.executionOnContainer(container.containerId, command: _*).execute()
    }

    nodesWithIpTablesModified ++= partitions.flatMap(_.nodes)
  }

  /* ! This is supposed to work only with DockerNodes, it will fail at runtime otherwise ! */
  def cancelIsolation(): Unit = {
    /* Filter with existing containers, this avoids failed cancellation or forgotten ones to break this method because of no more existing containers id */
    val runningIds = Docker.listContainers().map(_.getId)
    nodesWithIpTablesModified.map(_.containerId).filter(runningIds.contains(_)).foreach { id =>
      Docker.executionOnContainer(id, "/bin/bash", "-c", s"$acceptAllRules && $saveIpTablesRules").execute()
    }
    nodesWithIpTablesModified = Seq()
  }

  def setLag(on: String, lag: FiniteDuration): Unit = {
    Docker.executionOnContainer(on, "tc", "qdisc", "add", "dev", "eth0", "root", "netem", "delay", s"${lag.toMillis}ms").execute()
  }

  def removeLag(on: String): Try[Array[String]] = {
    Docker.executionOnContainer(on, "tc", "qdisc", "del", "dev", "eth0", "root", "netem").execute()
  }

  def setLag(from: String, to: String, lag: FiniteDuration, bidirectional: Boolean = false): Unit = {
    val ip = Docker.getIp(to)
    setLagInternal(from, ip, lag)

    if (bidirectional) {
      setLag(to, from, lag)
    }
  }

  /**
    * Add some lag between a container and some other host
    *
    * @param on           the container id on which to add the lag
    * @param externalHost the external host to add lag to. It can be a hostname or an IP
    */
  def setLagToExternalHost(on: String, externalHost: String, lag: FiniteDuration): Unit = setLagInternal(on, resolveIp(externalHost), lag)

  private def setLagInternal(on: String, ip: String, lag: FiniteDuration): Unit = {
    Docker.executionOnContainer(on, "/bin/bash", "-c",
      s"""tc qdisc add dev eth0 root handle 1: prio
         | && tc filter add dev eth0 protocol ip parent 1:0 prio 3 u32 match ip dst $ip flowid 1:3
         | && tc qdisc add dev eth0 parent 1:3 handle 30: netem  delay ${lag.toMillis}ms""".stripMargin.replaceAll("\n", "")).execute()
  }

  /**
    * Block an external host from a container
    *
    * @param on           the container id on which to add networking rules
    * @param externalHost the external host to block. It can be a hostname or an IP
    */
  def blockExternalHost(on: String, externalHost: String): Unit = {
    val ip = resolveIp(externalHost)

    Docker.executionOnContainer(on, "/bin/bash", "-c",
      s"""$acceptAllRules
         | && iptables -A INPUT -d $ip -j DROP
         | && iptables -A OUTPUT -d $ip -j DROP
         | && iptables -A FORWARD -d $ip -j DROP
         | $saveIpTablesRules""".stripMargin.replaceAll("\n", "")).execute()
  }

  private def resolveIp(nameOrIp: String): String = InetAddress.getByName(nameOrIp).getAddress.map(_.toInt & 0xff).mkString(".")
}
