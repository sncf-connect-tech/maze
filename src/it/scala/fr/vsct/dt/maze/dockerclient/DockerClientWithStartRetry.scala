package fr.vsct.dt.maze.dockerclient

import java.util.concurrent.ConcurrentHashMap

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command._

import scala.collection.JavaConverters._
import scala.collection.concurrent.Map


class FakeRetryException(message: String) extends RuntimeException((message))


class DockerClientWithStartRetry(override val client: DockerClient, nbFailByStart: Int = 0) extends DockerClientWrapper(client) {

  val mapFailByStart: Map[String, Int] = new ConcurrentHashMap[String, Int]().asScala

  override def startContainerCmd(containerId: String): StartContainerCmd = {

    mapFailByStart.putIfAbsent(containerId, nbFailByStart)

    val nbRetry = mapFailByStart(containerId)
    if (nbRetry > 0) {
      mapFailByStart.update(containerId, nbRetry - 1)
      throw new FakeRetryException("Fake failure from DockerClientWithStartRetry")
    }

    client.startContainerCmd(containerId)
  }
}
