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
import fr.vsct.dt.maze.core.Commands.{expectThat, waitUntil}
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.core.{Execution, Result}
import fr.vsct.dt.maze.topology.{Docker, SingleContainerClusterNode}

import scala.concurrent.duration._
import scala.language.postfixOps

class LogsTest extends TechnicalTest {

  var container: SingleContainerClusterNode = 1.node named "echo" constructedLike new SingleContainerClusterNode {
    override def serviceContainer: CreateContainerCmd = "busybox".withEntrypoint("/bin/sh", "-c", "echo aaaaa && echo bbbbb")
    override def servicePort: Int = 1234
  } buildSingle()

  override protected def beforeEach(): Unit = {
    container.start()
  }

  "a container log" should "be returned on several lines" in {
    waitUntil(Execution{
      Docker.containerInfo(container.containerId).getState.getRunning
    }.toPredicate(s"container ${container.containerId} is running?") {
      case stopped if !stopped => Result.success
      case running => Result.failure(s"container.getRunning returned $running")
    }) butNoLongerThan (5 seconds)
    expectThat(container.logs.map(_.mkString("[", ",", "]")) is "[aaaaa,bbbbb]")
  }

  override protected def afterEach(): Unit = {
    container.clear()
  }
}
