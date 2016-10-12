package com.vsct.dt.dsl.topology

import com.vsct.dt.dsl.core.Execution

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

  /**
    * Convenient way to group nodes together
    *
    * @return a list of nodes
    */
  def ::(otherNode: ClusterNode): List[ClusterNode] = {
    List(this, otherNode)
  }

  override def toString: String = hostname

}
