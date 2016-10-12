package com.vsct.dt.dsl.helpers

import java.net.InetAddress

import com.vsct.dt.dsl.topology.{Docker, DockerClusterNode, NodeGroup}

import collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object DockerNetwork {

  val networkName = "technical-tests"
  val subnet = "10.20.0.0/16"

  var nodesWithIpTablesModified: Seq[DockerClusterNode] = Seq()

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
  private val dropAllRules = s"iptables -A INPUT -d $subnet -j DROP && iptables -A OUTPUT -d $subnet -j DROP && iptables -A FORWARD -d $subnet -j DROP"
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

    val ips: Map[String, String] = everything.map(node => (node.containerId, Docker.containerInfo(node.containerId).getNetworkSettings.getNetworks.get(networkName).getIpAddress)).toMap

    // Reformat the data to get a map as such: Map[<container id>, Set[<invisible ip address>]]
    val unreacheables: Map[String, Set[String]] =
      (
        for {
          container <- everything
          partition <- partitions.map(_.nodes) if !partition.contains(container)
          otherContainer <- partition
        } yield (container.containerId, ips(otherContainer.containerId))
        ).groupBy(_._1)
        // groupBy will return a Map[String, Set[(String, String)]], transform it to a Map[String, Set[String]]
        .map(tuple => (tuple._1, tuple._2.map(_._2)))

    unreacheables.foreach { entry =>
      val container = entry._1
      val ips = entry._2

      val commands = ips.toSeq.map { ip =>
        s"iptables -A INPUT -d $ip -j DROP && iptables -A OUTPUT -d $ip -j DROP && iptables -A FORWARD -d $ip -j DROP"
      }.mkString(" && ")

      val command: Seq[String] = Seq("/bin/bash", "-c", s"$acceptAllRules && $commands && $saveIpTablesRules")
      Docker.executionOnContainer(container, command: _*).execute()
    }

    nodesWithIpTablesModified ++= partitions.flatMap(_.nodes)
  }

  /* Each provided Seq[DockerClusterNode] will be isolated from the rest of the world
     The main difference with split is that nodes not included in such Seq will not see the isolated nodes
     Note that network traffic comming from outside the subnet wil still reach these nodes, allowing controll from outside
   */
  def isolate(partitions: Seq[DockerClusterNode]*): Unit = {

    // This only works in privileged mode.
    // If ip is a node from the same partition, in order to change the firewall rules, we want to execute (network is the subnet) :
    // iptables -F (flush the iptables rules)
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

    val nodesAndTheirBuddiesIP: Map[String, Seq[String]] = (for {
      partition <- partitions
      container <- partition
      otherContainer <- partition
    } yield (container.containerId, otherContainer.containerId)
      ).groupBy(_._1)
    // groupBy will return a Map[String, Set[(String, String)]], transform it to a Map[String, Set[String]]
     .map(tuple => (tuple._1, tuple._2.map(tuple2 => Docker.containerInfo(tuple2._2).getNetworkSettings.getNetworks.get(networkName).getIpAddress)))

    nodesAndTheirBuddiesIP.foreach( entry => {
      val containerId = entry._1
      val ips = entry._2

      val commands = ips.map { ip =>
        s"iptables -A INPUT -d $ip -j ACCEPT && iptables -A OUTPUT -d $ip -j ACCEPT && iptables -A FORWARD -d $ip -j ACCEPT"
      }.mkString(" && ")

      val command = Seq("/bin/bash", "-c", s"$acceptAllRules && $commands && $dropAllRules && $saveIpTablesRules")
      Docker.executionOnContainer(containerId, command: _*).execute()
    })

    nodesWithIpTablesModified ++= partitions.flatten
  }

  /* ! This is supposed to work only with DockerNodes, it will fail at runtime otherwise ! */
  def cancelIsolation(): Unit = {
    /* Filter with existing containers, this avoids failed cancelation or forgotten ones to break this method because of no more existing containers id */
    nodesWithIpTablesModified.filter(Docker.listContainers().contains(_)).foreach { c =>
      Docker.executionOnContainer(c.containerId, "/bin/bash", "-c", s"$acceptAllRules && $saveIpTablesRules").execute()
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
