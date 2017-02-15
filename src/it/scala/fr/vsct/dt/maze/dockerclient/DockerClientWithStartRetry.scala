package fr.vsct.dt.maze.dockerclient

import java.io.{File, InputStream}
import java.util.concurrent.ConcurrentHashMap

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command._
import com.github.dockerjava.api.model.{AuthConfig, Identifier}

import scala.collection.JavaConverters._
import scala.collection.concurrent.Map


class FakeRetryException(message: String) extends RuntimeException((message)){
  //def this( msg: String ) = this( msg )
}


class DockerClientWithStartRetry(val client: DockerClient, nbFailByStart: Int = 0) extends DockerClient {

  val mapFailByStart: Map[String, Int] = new ConcurrentHashMap[String, Int]().asScala

  override def killContainerCmd(containerId: String): KillContainerCmd = ???

  override def logContainerCmd(containerId: String): LogContainerCmd = ???

  override def updateContainerCmd(containerId: String): UpdateContainerCmd = ???

  override def copyFileFromContainerCmd(containerId: String, resource: String): CopyFileFromContainerCmd = ???

  override def restartContainerCmd(containerId: String): RestartContainerCmd = ???

  override def inspectVolumeCmd(name: String): InspectVolumeCmd = ???

  override def pauseContainerCmd(containerId: String): PauseContainerCmd = ???

  override def createNetworkCmd(): CreateNetworkCmd = ???

  override def statsCmd(containerId: String): StatsCmd = ???

  override def authCmd(): AuthCmd = ???

  override def commitCmd(containerId: String): CommitCmd = ???

  override def attachContainerCmd(containerId: String): AttachContainerCmd = ???

  override def searchImagesCmd(term: String): SearchImagesCmd = ???

  override def listContainersCmd(): ListContainersCmd = client.listContainersCmd()

  override def topContainerCmd(containerId: String): TopContainerCmd = ???

  override def inspectImageCmd(imageId: String): InspectImageCmd = ???

  override def infoCmd(): InfoCmd = ???

  override def disconnectFromNetworkCmd(): DisconnectFromNetworkCmd = ???

  override def removeVolumeCmd(name: String): RemoveVolumeCmd = ???

  override def inspectContainerCmd(containerId: String): InspectContainerCmd = client.inspectContainerCmd(containerId)

  override def execStartCmd(execId: String): ExecStartCmd = ???

  override def pullImageCmd(repository: String): PullImageCmd = ???

  override def pingCmd(): PingCmd = ???

  override def startContainerCmd(containerId: String): StartContainerCmd = {

    mapFailByStart.putIfAbsent(containerId, nbFailByStart)

    val nbRetry = mapFailByStart(containerId)
    if (nbRetry > 0) {
      mapFailByStart.update(containerId, nbRetry - 1)
      throw new FakeRetryException("Fake failure from DockerClientWithStartRetry")
    }

    client.startContainerCmd(containerId)
  }

  override def copyArchiveFromContainerCmd(containerId: String, resource: String): CopyArchiveFromContainerCmd = ???

  override def versionCmd(): VersionCmd = ???

  override def copyArchiveToContainerCmd(containerId: String): CopyArchiveToContainerCmd = client.copyArchiveToContainerCmd(containerId)

  override def authConfig(): AuthConfig = ???

  override def removeImageCmd(imageId: String): RemoveImageCmd = ???

  override def waitContainerCmd(containerId: String): WaitContainerCmd = ???

  override def loadImageCmd(imageStream: InputStream): LoadImageCmd = ???

  override def tagImageCmd(imageId: String, repository: String, tag: String): TagImageCmd = ???

  override def removeContainerCmd(containerId: String): RemoveContainerCmd = client.removeContainerCmd(containerId)

  override def saveImageCmd(name: String): SaveImageCmd = ???

  override def close(): Unit = ???

  override def listNetworksCmd(): ListNetworksCmd = ???

  override def createVolumeCmd(): CreateVolumeCmd = ???

  override def removeNetworkCmd(networkId: String): RemoveNetworkCmd = ???

  override def stopContainerCmd(containerId: String): StopContainerCmd = ???

  override def createImageCmd(repository: String, imageStream: InputStream): CreateImageCmd = ???

  override def containerDiffCmd(containerId: String): ContainerDiffCmd = ???

  override def inspectExecCmd(execId: String): InspectExecCmd = ???

  override def connectToNetworkCmd(): ConnectToNetworkCmd = ???

  override def pushImageCmd(name: String): PushImageCmd = ???

  override def pushImageCmd(identifier: Identifier): PushImageCmd = ???

  override def inspectNetworkCmd(): InspectNetworkCmd = ???

  override def eventsCmd(): EventsCmd = ???

  override def execCreateCmd(containerId: String): ExecCreateCmd = ???

  override def renameContainerCmd(containerId: String): RenameContainerCmd = ???

  override def listImagesCmd(): ListImagesCmd = client.listImagesCmd()

  override def createContainerCmd(image: String): CreateContainerCmd = client.createContainerCmd(image)

  override def unpauseContainerCmd(containerId: String): UnpauseContainerCmd = ???

  override def buildImageCmd(): BuildImageCmd = ???

  override def buildImageCmd(dockerFileOrFolder: File): BuildImageCmd = ???

  override def buildImageCmd(tarInputStream: InputStream): BuildImageCmd = ???

  override def listVolumesCmd(): ListVolumesCmd = ???
}
