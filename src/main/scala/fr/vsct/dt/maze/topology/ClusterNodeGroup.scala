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

import fr.vsct.dt.maze.helpers.DockerNetwork

import scala.collection.mutable

class ClusterNodeGroupBuilder[T <: DockerClusterNode](val nodes: Seq[T]){
  def as(name: String):ClusterNodeGroup[T] = new ClusterNodeGroup[T](name, nodes)
}

class ClusterNodeGroup[+T <: DockerClusterNode](val name: String, val nodes: Seq[T]) {

  ClusterNodeGroup.register(this)

  def isolate(): Unit = {
    DockerNetwork.isolate(this)
  }
}

object ClusterNodeGroup {

  val groups: mutable.HashMap[String, ClusterNodeGroup[DockerClusterNode]] = mutable.HashMap()

  def register[T <: DockerClusterNode](group: ClusterNodeGroup[T]): Option[ClusterNodeGroup[DockerClusterNode]] = {
    this.groups.put(group.name, group)
  }

  def get[T <: DockerClusterNode](name: String):ClusterNodeGroup[T] = {
    groups.getOrElse(name, new ClusterNodeGroup[T](name, Seq())).asInstanceOf[ClusterNodeGroup[T]]
  }

}
