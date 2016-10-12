package com.vsct.dt.maze.topology

import com.vsct.dt.maze.core.{Commands, Execution, Predicate}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag


/**
  * Simple cluster abstraction
  *
  * @tparam T the type of nodes, must be a subclass of com.vsct.dt.dsl.topology.ClusterNode
  */
abstract class Cluster[T <: ClusterNode : ClassTag](var nodes: Seq[T] = Seq(), parallel: Boolean = true) {

  def start(): Unit = {
    beforeStart()

    if (parallel) nodes.par.foreach(_.start())
    else nodes.foreach { node =>
      node.start()
      Commands.waitFor(2 seconds)
    }

    afterStart()
  }

  def beforeStart(): Unit = {}

  def afterStart(): Unit = {}

  def stop(): Unit = {
    nodes.foreach(_.clear())
  }

  def add(nodeBuilder: MultipleClusterNodeBuilder[T]): Seq[T] = {
    generateInternal(nodeBuilder)
  }

  def add(nodeBuilder: SingleClusterNodeBuilder[T]): T = {
    generateInternal(nodeBuilder).head
  }

  private def generateInternal(nodeBuilder: ClusterNodeBuilder[T])(implicit ct: ClassTag[T]): Seq[T] = {
    //generate names first, so things like connection strings or any combination of the generated names that can be useful for the nodes themselves can be used in constructor
    val names = 1 to nodeBuilder.numberOfNodes map { _ => nodeBuilder.hostnameBuilder() }
    val newNodes = names map { hostname =>
      val node: T = nodeBuilder.nodeBuilder(names)
      node.hostname = hostname
      node
    }
    nodes = nodes ++ newNodes
    newNodes
  }

  /**
    * Basic implementation of a connection string: list of all hostname:servicePort separated with ','
    * This method would be called to configure containers calling other containers in the same network
    *
    * @return A connexion string that can be part of what clients need to use the cluster
    */
  def internalConnectionString: String = nodes.map(n => s"${n.hostname}:${n.servicePort}").mkString(",")

  def getNode(hostname: String): T = {
    nodes.find(_.hostname == hostname).get
  }

  def getNodeFromIp(ip: String): T = {
    nodes.find(_.ip == ip).get
  }

  def find_the_node_which(fn: T => Predicate): Execution[T] = {
    Execution(() => nodes.find(node => fn(node).execute())).map(_.get).labeled("node matching predicate")
  }

  def filter_the_nodes_with_condition(fn: (T) => Predicate): Execution[Array[T]] = {
    Execution(() => nodes.toArray.filter(node => fn(node).execute())).labeled("nodes filtered")
  }

}

/* Not used yet, we will have to decide if it is useful */
abstract class DockerCluster[T <: DockerClusterNode : ClassTag] extends Cluster[T] {
  /**
    * Build a connexion string to the cluster using mapped ports on the docker host.
    * The members of the cluster not binding ports on localhost will be ignored.
    * This method would be called to configure actors on localhost calling containers on another network
    *
    * @return a connection string to call the cluster with ports bound on localhost
    */
  def externalConnectionString: String = {
    // Use flatMap to filter nodes with no internal port
    nodes.flatMap(_.mappedPort).map(port => s"${Docker.host.nameOrIp}:$port").mkString(",")
  }
}


// Builder classes to add nodes to a cluster

class SingleClusterNodeBuilderStepOne {
  def named(hostnamePrefix: String): SingleClusterNodeBuilderStepTwo = {
    new SingleClusterNodeBuilderStepTwo(hostnamePrefix)
  }
}

class SingleClusterNodeBuilderStepTwo(val hostnamePrefix: String) {
  def constructedLike[T <: ClusterNode](nodeBuilder: Seq[String] => T): SingleClusterNodeBuilder[T] = {
    new SingleClusterNodeBuilder[T](() => hostnamePrefix + "-" + HostNames.getNextIndex(hostnamePrefix), nodeBuilder)
  }
}

class MultipleClusterNodeBuilderStepOne(val numberOfNodes: Int) {
  def named(hostnamePrefix: String): MultipleClusterNodeBuilderStepTwo = {
    new MultipleClusterNodeBuilderStepTwo(numberOfNodes, hostnamePrefix)
  }
}

class MultipleClusterNodeBuilderStepTwo(val numberOfNodes: Int, val hostnamePrefix: String) {
  def constructedLike[T <: ClusterNode](nodeBuilder: Seq[String] => T): MultipleClusterNodeBuilder[T] = {
    new MultipleClusterNodeBuilder[T](numberOfNodes, () => hostnamePrefix + "-" + HostNames.getNextIndex(hostnamePrefix), nodeBuilder)
  }
}

trait ClusterNodeBuilder[T <: ClusterNode] {
  def numberOfNodes: Int = 1

  def hostnameBuilder: () => String

  def nodeBuilder: Seq[String] => T
}

class MultipleClusterNodeBuilder[T <: ClusterNode](
                                                    override val numberOfNodes: Int,
                                                    override val hostnameBuilder: () => String,
                                                    override val nodeBuilder: Seq[String] => T) extends ClusterNodeBuilder[T]

class SingleClusterNodeBuilder[T <: ClusterNode](
                                                  override val hostnameBuilder: () => String,
                                                  override val nodeBuilder: Seq[String] => T) extends ClusterNodeBuilder[T]

