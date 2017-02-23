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
 *
 *//*
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
 *
 */
package fr.vsct.dt.maze.dockerclient

import java.io.InputStream
import java.lang.Boolean
import java.util.{Timer, TimerTask}
import javax.annotation.Nonnull

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command._
import com.github.dockerjava.api.model.Frame

class DockerClientWithDelayOnStartCmd(override val client: DockerClient, val delay: Int) extends DockerClientWrapper(client) {

  override def execStartCmd(execId: String): ExecStartCmd = {
    val command: ExecStartCmd = client.execStartCmd(execId)

    new ExecStartCmd() {
      def getExecId: String = command.getExecId

      def hasDetachEnabled: Boolean = command.hasDetachEnabled

      def hasTtyEnabled: Boolean = command.hasTtyEnabled

      def getStdin: InputStream = command.getStdin

      def withDetach(detach: Boolean): ExecStartCmd = command.withDetach(detach)

      def withExecId(@Nonnull execId: String): ExecStartCmd = command.withExecId(execId)

      def withTty(tty: Boolean): ExecStartCmd = command.withTty(tty)

      def withStdIn(stdin: InputStream): ExecStartCmd = command.withStdIn(stdin)

      def exec[T <: ResultCallback[Frame]](resultCallback: T): T = {
        val timer: Timer = new Timer
        timer.schedule(new TimerTask() {
          def run(): Unit = {
            command.exec(resultCallback)
          }
        }, delay)
        resultCallback
      }

      def close(): Unit = command.close()
    }
  }
}