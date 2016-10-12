package com.vsct.dt.dsl.topology

import scala.collection.mutable

class ClusterNodeGroupBuilder[T <: ClusterNode](val nodes: Seq[T]){
  def as(name: String):ClusterNodeGroup[T] = {
    new ClusterNodeGroup[T](name, nodes)
  }
}

class ClusterNodeGroup[+T <: ClusterNode](val name: String, val nodes: Seq[T]) {

  ClusterNodeGroup.register(this)

  def length: Int = nodes.length

  def apply(index: Int): T = nodes(index)

  def iterator: Iterator[T] = nodes.iterator
}

object ClusterNodeGroup {

  val groups: mutable.HashMap[String, ClusterNodeGroup[ClusterNode]] = mutable.HashMap()

  def register[T <: ClusterNode](group: ClusterNodeGroup[T]): Option[ClusterNodeGroup[ClusterNode]] = {
    this.groups.put(group.name, group)
  }

  def apply[T <: ClusterNode](name: String):ClusterNodeGroup[T] = {
    groups.getOrElse(name, new ClusterNodeGroup[T](name, Seq())).asInstanceOf[ClusterNodeGroup[T]]
  }

  def apply[T <: ClusterNode](name: String, nodes:Seq[T]) = new ClusterNodeGroup[T](name, nodes)
}
