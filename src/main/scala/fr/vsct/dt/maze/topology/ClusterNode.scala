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

import fr.vsct.dt.maze.core.Execution

import scala.concurrent.duration.FiniteDuration

/**
  * This is an abstraction to describe a node of a cluster, handled by docker.
  *
  * To use it at ease, see com.vsct.dt.dsl.MultipleContainerClusterNode or com.vsct.dt.dsl.SingleContainerClusterNode
  */
trait ClusterNode {

  /**
    * Start node
    */
  def start(): Unit

  /**
    * Stop node
    */
  def stop(): Unit

  /**
    * Trash node : stop it (do not expect it to be stopped cleanly) and remove metadata from it.
    *
    * A cleared node cannot be restarted.
    */
  def clear(): Unit

  /**
    * Stop and start a node
    */
  def restart(): Unit = {
    stop()
    start()
  }

  /**
    * Kill a node, sending a given signal
    *
    * @param signal the unix signal to send, by default SIGTERM
    */
  def kill(signal: String = "SIGTERM"): Unit

  /**
    * Stop cleanly the node by calling the shutdown script.
    */
  def stopCleanly(): Unit

  /**
    * Crash violently the process.
    */
  def crash(): Unit

  /**
    * Return the complete logs for this node
    *
    * @return a way to retrieve the logs, as an array
    */
  def logs: Execution[Array[String]]

  /**
    * Return an execution of the complete logs associated with the hostname
    * @return
    */
  def logsWithName: Execution[(String, Array[String])] = logs.map((hostname, _))

  /**
    * Execute some command on a shell
    *
    * @param command the command to execute, every parameter is a word (for instance: "ps", "aux")
    * @return the result of the command as a list of lines
    */
  def shellExecution(command: String*): Execution[Array[String]]

  /**
    * Lag on the whole node of given duration.
    *
    * @param duration of the added lag
    */
  def lag(duration: FiniteDuration): Unit

  /**
    * Fill with a file of the size of the remaining space in the given path.
    *
    * @param path the path to the partition to fill
    */
  def fillFilesystem(path: String): Unit = shellExecution("dd", "if=/dev/zero", s"of=$path/fill-it", "bs=$((1024*1024))", "count=$((1024*1024))").execute()

  /**
    * Getter for the hostname of this node
    *
    * @return the hostname, used to call this node from inside the network
    */
  var hostname: String

  /**
    * The service port is the main port of the application. For instance, on tomcat, this port usually is 8080.
    * This is the port that will be used, for instance, to create connection strings
    *
    * @return the service port
    */
  def servicePort: Int

  /**
    * The IP of the node
    */
  def ip: String

  /**
    * Create a file with a given content on the container
    *
    * @param path    the path of the file to create
    * @param content the content of the file
    */
  def createFile(path: String, content: String): Unit

  override def toString: String = hostname

}
