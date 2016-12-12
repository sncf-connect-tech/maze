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

object HostNames {

  /* Stores current indexes for hostname. Useful to avoid hostname collision when the hostname is user predefined */
  private var hostnameIndexes: Map[String, Int] = Map()

  def getNextIndex(hostName: String): Int = {
    val index = hostnameIndexes.getOrElse(hostName, -1)
    hostnameIndexes += (hostName -> (index + 1))
    index + 1
  }

}
