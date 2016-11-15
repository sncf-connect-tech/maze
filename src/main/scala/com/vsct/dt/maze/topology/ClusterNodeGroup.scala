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

package com.vsct.dt.maze.topology

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

  def apply[T <: ClusterNode](name: String, nodes:Seq[T]): ClusterNodeGroup[T] = new ClusterNodeGroup[T](name, nodes)
}
