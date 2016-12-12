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

package fr.vsct.dt.maze.topology

import java.util.concurrent.atomic.AtomicInteger

import com.github.dockerjava.api.command.{CreateContainerCmd, InspectContainerResponse}
import com.github.dockerjava.api.model.{PortBinding, VolumesFrom}
import com.typesafe.scalalogging.StrictLogging
import fr.vsct.dt.maze.helpers.DockerNetwork

import collection.JavaConverters._

abstract class MultipleContainerClusterNode extends DockerClusterNode with StrictLogging {

  private[this] var mappedServicePort: Option[Int] = None
  private[this] var mappedServiceIp: String = ""
  private[this] var containerIp: String = ""

  private[this] var id: String = _
  private[this] var dataIds: Seq[String] = _

  override def containerId: String = id

  //final hostname is set by the builder
  var hostname: String = ""

  def dataContainers: Seq[CreateContainerCmd]

  override def start(): Unit = {
    if (hasBeenStartedOnce()) {
      startStoppedNode()
    }
    else {
      startFirstTime()
    }
  }

  override def stop(): Unit = {
    Docker.stopContainer(containerId)
  }

  override def clear(): Unit = {
    Docker.forceRemoveContainer(containerId)
    dataIds.foreach(Docker.forceRemoveContainer)
  }

  private def hasBeenStartedOnce(): Boolean = Option(containerId).isDefined

  private def startStoppedNode(): Unit = Docker.startCreatedContainer(containerId)

  private def startFirstTime(): Unit = {
    beforeStart()

    val counter = new AtomicInteger()

    val dataContainersWithName = dataContainers.map {
      _.withName(s"$hostname-data-${counter.getAndIncrement()}")
    }

    val names = dataContainersWithName.map(c => new VolumesFrom(c.getName))


    dataIds = dataContainersWithName.map(Docker.createAndStartContainer)
    var command: CreateContainerCmd = serviceContainer.withHostName(hostname)
      // Keep user defined bindings
      .withPortBindings(
      (Docker.constructBinding(servicePort) ::
        Option(serviceContainer.getPortBindings)
          .map(_.getBindings.asScala.toList.flatMap { case (port, ips) => ips.map(bind => new PortBinding(bind, port)) })
          .getOrElse(List())).asJava
    )
      .withName(hostname)

    if (names.nonEmpty) {
      command = command.withVolumesFrom(names: _*)
    }
    id = Docker.createAndStartContainer(command)

    val info = Docker.containerInfo(containerId)
    mappedServicePort = getMappedPort(servicePort, info)
    mappedServiceIp = getMappedIp(servicePort, info)
    containerIp = info.getNetworkSettings.getNetworks.get(DockerNetwork.networkName).getIpAddress

    if (!Option(info.getState.getRunning).exists(_.booleanValue())) {
      logger.warn(s"Container $hostname is not running, state is ${info.getState.getStatus}")
    }

    afterStart(info)
  }

  override def externalIp: String = mappedServiceIp

  override def mappedPort: Option[Int] = mappedServicePort

  override def internalIp: String = containerIp

  def serviceContainer: CreateContainerCmd

  def beforeStart(): Unit = {}

  def afterStart(infos: InspectContainerResponse): Unit = {}

  /**
    * Retrieve the ip on which a port is mapped
    *
    * @param internalPort the port to look for
    * @param info         the result to docker inspect in which to look for the information
    * @return the IP on which a given port is mapped
    */
  def getMappedIp(internalPort: Int, info: InspectContainerResponse): String = {
    Option(info.getNetworkSettings.getPorts)
      .flatMap(
        _.getBindings.asScala.find { case (port, _) => Option(port.getPort).contains(internalPort) }
          .flatMap { case (_, bindings) => Option(bindings) }
          .map(_.toList)
          .flatMap(_.headOption)
          .map(_.getHostIp)
          .filter(_ != "0.0.0.0")
      )
      .getOrElse(Docker.host.nameOrIp)
  }

  /**
    * Retrieve the port mapping to some internal port
    *
    * @param internalPort the port to look for
    * @param info         the result to docker inspect in which to look for the information
    * @return the port mapping to some given internal port
    */
  def getMappedPort(internalPort: Int, info: InspectContainerResponse): Option[Int] = {
    Option(info.getNetworkSettings.getPorts)
      .flatMap(
        _.getBindings.asScala.find { case (port, _) => Option(port.getPort).contains(internalPort) }
          .flatMap { case (_, bindings) => Option(bindings) }
          .map(_.toList)
          .flatMap(_.headOption)
      )
      .map(_.getHostPortSpec.toInt)
  }

}
