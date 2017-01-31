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

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.PortBinding
import fr.vsct.dt.maze.core.Execution
import fr.vsct.dt.maze.helpers.DockerNetwork

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

trait DockerClusterNode extends ClusterNode{

  implicit def stringToPrepareContainer(image: String): CreateContainerCmd = Docker.prepareCreateContainer(image)

  implicit def stringToPortBinding(binding: String): PortBinding = PortBinding.parse(binding)

  implicit def portToPortBinding(port: Int): PortBinding = Docker.constructBinding(port)

  /**
    * Kill a node, sending a given signal
    *
    * @param signal the unix signal to send, by default SIGTERM
    */
  override def kill(signal: String = "SIGTERM"): Unit = Docker.killContainer(containerId, signal)

  /**
    * Stop cleanly the node by calling the shutdown script.
    */
  override def stopCleanly(): Unit = kill(signal = "SIGTERM")

  /**
    * Crash violently the kafka process.
    */
  override def crash(): Unit = kill(signal = "KILL")

  /**
    * Return the id of the main container (for instance the tomcat)
    *
    * @return the id of the main container (for instance the tomcat)
    */
  def containerId: String

  /**
    * Execute some command on the main container
    *
    * @param command the command to execute, every parameter is a word (for instance: "ps", "aux")
    * @return the result of the command as a list of lines
    */
  override def shellExecution(command: String*): Execution[Array[String]] = Docker.executionOnContainer(containerId, command: _*)

  /**
    * Lag on the whole node of given duration.
    *
    * @param duration of the added lag
    */
  override def lag(duration: FiniteDuration): Unit = DockerNetwork.setLag(on = containerId, lag = duration)

  /**
    * Return the port that maps on the host to the service port
    *
    * @return the mapped port
    */
  def mappedPort: Option[Int]

  /**
    * Gets the internal ip of the container
    *
    * @return the internal ip of the container, to use for communication between containers
    */
  def internalIp: String

  /** For docker implementation we decide that ip will be the internal ip of the node
    *
    */
  override def ip: String = internalIp

  /**
    * Gets the external ip of the container
    *
    * @return the external ip of the container, to use for communication between this container and the orchestrator
    */
  def externalIp: String

  /**
    * Return the complete logs for this node
    *
    * @return a way to retrieve the logs, as an array
    */
  override def logs: Execution[Array[String]] = Docker.logs(containerId)
  /**
    * Create a file with a given content on the container
    *
    * @param path    the path of the file to create
    * @param content the content of the file
    */
  override def createFile(path: String, content: String): Unit = Docker.executionCreateFileOnContainer(containerId, path, content)

}
