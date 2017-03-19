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

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerCmd
import fr.vsct.dt.maze.core.Commands.{expectThat, waitUntil}
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.dockerclient.{DockerClientWithStartRetry, FakeRetryException}
import fr.vsct.dt.maze.helpers.Http.HttpEnabled
import fr.vsct.dt.maze.topology.{Docker, DockerCluster, SingleContainerClusterNode}

import scala.concurrent.duration._
import scala.language.postfixOps
import collection.JavaConverters._

class CreateAndStartWithRetryTest extends TechnicalTest {

  class NginxClusterNode extends SingleContainerClusterNode with HttpEnabled {
    override def serviceContainer: CreateContainerCmd = "nginx"

    override def servicePort: Int = 80
  }

  var preservedDockerClient: DockerClient = _

  var cluster: DockerCluster[NginxClusterNode] = _

  override protected def beforeEach(): Unit = {
    preservedDockerClient = Docker.client
    cluster = new DockerCluster[NginxClusterNode] {}
    cluster.add(1.nodes named "nginx" constructedLike new NginxClusterNode)
  }

  override protected def afterEach(): Unit = {
    cluster.stop()
    Docker.client = preservedDockerClient
  }

  private def expectNginxclusterIsOk(): Unit = {
    cluster.start()
    cluster.nodes.foreach { node => waitUntil(node.httpGet("/").status is 200) butNoLongerThan (10 seconds) }

    val firstNode = cluster.nodes.head

    firstNode.createFile("/usr/share/nginx/html/test.txt", "ok")
    expectThat(firstNode.httpGet("/test.txt").response is "ok")
  }

  "a nginx" should "do all that's needed when there is no retry" in {
    Docker.client = new DockerClientWithStartRetry(Docker.client)
    expectNginxclusterIsOk
  }

  it should "do all that's needed when there is 1 retry" in {
    Docker.client = new DockerClientWithStartRetry(Docker.client, 1)
    expectNginxclusterIsOk
  }

  it should "fail when there is more fail than retry number" in {
    Docker.client = new DockerClientWithStartRetry(Docker.client, Int.MaxValue)

    assertThrows[FakeRetryException] {
      cluster.start()
    }

    val cmd = Docker.client.listContainersCmd.withLabelFilter(Docker.extraConfiguration.extraLabels.asJava).withShowAll(true)
    Docker.listContainers(cmd).filter(_.getStatus == "Created").foreach(c => Docker.forceRemoveContainer(c.getId))
    cluster = new DockerCluster[NginxClusterNode] {}
  }
}