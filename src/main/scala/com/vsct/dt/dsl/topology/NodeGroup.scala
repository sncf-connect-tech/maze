package com.vsct.dt.dsl.topology

class NodeGroup(val nodes: Seq[DockerClusterNode]) {

  def +(other: NodeGroup): NodeGroup = new NodeGroup(this.nodes ++ other.nodes)
  def +(other: DockerClusterNode): NodeGroup = new NodeGroup(this.nodes ++ Seq(other))

}
