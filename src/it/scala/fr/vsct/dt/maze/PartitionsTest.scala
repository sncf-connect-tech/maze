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

import java.io.File

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.core.command.BuildImageResultCallback
import fr.vsct.dt.maze.core.Commands.{expectThat, _}
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.core.{Predicate, Result}
import fr.vsct.dt.maze.helpers.DockerNetwork
import fr.vsct.dt.maze.helpers.Http.HttpEnabled
import fr.vsct.dt.maze.topology.{Docker, SingleContainerClusterNode}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
  * Created by tokou on 18/03/2017.
  */
class PartitionsTest extends TechnicalTest {

  private val imageName = "iptables-curl-nginx:1.0"

  /*
     * A node used to test paritions. Needs an 'iptables-curl-nginx' image built using
     * the partitions/Dockerfile docker file that adds iptables and curl.
     */
  class PartitionNode extends SingleContainerClusterNode with HttpEnabled {
    override def serviceContainer: CreateContainerCmd = imageName
    override val servicePort: Int = 80

    def canCommunicateWith(other: PartitionNode): Predicate = {
      canCurl(other) && other.canCurl(this)
    }

    private def canCurl(other: PartitionNode) : Predicate = {
      doCurl(other)
        .map(_.head)
        .toPredicate(s"$hostname can curl ${other.hostname}") {
          case line if line contains "200 OK" => Result.success
          case line => Result.failure(s"could not curl, received $line")
        }
    }

    private def doCurl(other: PartitionNode) = {
      val ip = other.internalIp
      val port = other.servicePort
      shellExecution("curl", "-Is", "--connect-timeout", "1", s"$ip:$port")
    }

    def cannotCommunicateWith(other: PartitionNode): Predicate = {
      doCurl(other).isError && other.doCurl(this).isError
    }
  }

  var firstNode : PartitionNode = _
  var secondNode : PartitionNode = _
  var thirdNode : PartitionNode = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    setupImage()
  }

  def setupImage(): Unit = {
    val file = new File("src/it/partitions/")
    Docker.client.buildImageCmd(file)
        .withTag(imageName)
        .exec(new BuildImageResultCallback).awaitImageId()
  }

  override protected def beforeEach(): Unit = {
    firstNode = 1.node named "first" constructedLike new PartitionNode buildSingle()
    secondNode = 1.node named "second" constructedLike new PartitionNode buildSingle()
    thirdNode = 1.node named "third" constructedLike new PartitionNode buildSingle()

    firstNode.start()
    secondNode.start()
    thirdNode.start()

    waitUntil(firstNode.httpGet("/").status is 200) butNoLongerThan(10 seconds)
    waitUntil(secondNode.httpGet("/").status is 200) butNoLongerThan(10 seconds)
    waitUntil(thirdNode.httpGet("/").status is 200) butNoLongerThan(10 seconds)
  }

  "three nodes" should "be able to communicate" in {
    expectThat(firstNode canCommunicateWith secondNode)
    expectThat(firstNode canCommunicateWith thirdNode)
    expectThat(thirdNode canCommunicateWith secondNode)
  }

  "a partition of three nodes in two and one" should "keep communication between the two" in {

    DockerNetwork.split(firstNode + secondNode, thirdNode)

    expectThat(firstNode canCommunicateWith secondNode)
  }

  it should "prevent communication with the one" in {

    DockerNetwork.split(firstNode + secondNode, thirdNode)

    expectThat(firstNode cannotCommunicateWith thirdNode)
    expectThat(secondNode cannotCommunicateWith  thirdNode)
  }

  "isolating one node" should "prevent it from communicating with the others" in {

    tag(secondNode).as("isolated").isolate()

    expectThat(firstNode cannotCommunicateWith secondNode)
    expectThat(secondNode cannotCommunicateWith thirdNode)
  }

  it should "not mess with communication between remaining nodes" in {

    tag(secondNode).as("isolated").isolate()

    expectThat(firstNode canCommunicateWith thirdNode)
  }

  "cancelling isolation" should "allow communication again" in {

    tag(secondNode).as("isolated").isolate()

    expectThat(firstNode cannotCommunicateWith  secondNode)
    expectThat(thirdNode cannotCommunicateWith  secondNode)

    DockerNetwork.cancelIsolation()

    expectThat(firstNode canCommunicateWith secondNode)
    expectThat(thirdNode canCommunicateWith secondNode)
  }

  "cancelling split brain" should "allow communication again" in {
    DockerNetwork.split(firstNode + secondNode, thirdNode)

    expectThat(firstNode cannotCommunicateWith thirdNode)
    expectThat(secondNode cannotCommunicateWith  thirdNode)

    DockerNetwork.cancelIsolation()

    expectThat(firstNode canCommunicateWith thirdNode)
    expectThat(secondNode canCommunicateWith  thirdNode)
  }

  override protected def afterEach(): Unit = {
    Try(firstNode.clear())
    Try(secondNode.clear())
    Try(thirdNode.clear())
  }
}
