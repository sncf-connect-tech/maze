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

package fr.vsct.dt.maze

import com.github.dockerjava.api.command.CreateContainerCmd
import fr.vsct.dt.maze.Helper.{NginxCluster, NginxClusterNode}
import fr.vsct.dt.maze.core.Commands.{expectThat, waitUntil}
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.helpers.Http.HttpEnabled
import fr.vsct.dt.maze.topology.{Cluster, SingleContainerClusterNode}

import scala.concurrent.duration._
import scala.language.postfixOps

class SingleClusterTest extends TechnicalTest {

  var cluster: NginxCluster = _

  "a nginx" should "do all that's needed" in {

    cluster.nodes.foreach{n =>
      expectThat(n.httpGet("/").status is 200)
    }

    val firstNode = cluster.nodes.head

    firstNode.createFile("/usr/share/nginx/html/test.txt", "ok")
    expectThat(firstNode.httpGet("/test.txt").response is "ok")
  }

  override protected def beforeEach(): Unit = {
    cluster = new NginxCluster
    cluster.add(2.nodes named "nginx" constructedLike new NginxClusterNode)
    cluster.start()

    cluster.nodes.foreach{node => waitUntil(node.httpGet("/").status is 200) butNoLongerThan(10 seconds)}
  }

  override protected def afterEach(): Unit = {
    cluster.stop()
  }
}

object Helper {

  class NginxClusterNode extends SingleContainerClusterNode with HttpEnabled {
    override def serviceContainer: CreateContainerCmd = "nginx"
    override val servicePort: Int = 80
  }

  class NginxCluster extends Cluster[NginxClusterNode] {}

}