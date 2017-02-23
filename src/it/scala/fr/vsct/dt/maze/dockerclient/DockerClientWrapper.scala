package fr.vsct.dt.maze.dockerclient

import java.io.{File, InputStream}

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command._
import com.github.dockerjava.api.model.{AuthConfig, Identifier}

/**
  * Created by yannick_lorenzati on 23/02/17.
  */
class DockerClientWrapper(val client: DockerClient) extends DockerClient {
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

  override def startContainerCmd(containerId: String): StartContainerCmd = client.startContainerCmd(containerId)

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

  override def inspectExecCmd(execId: String): InspectExecCmd = client.inspectExecCmd(execId)

  override def connectToNetworkCmd(): ConnectToNetworkCmd = ???

  override def pushImageCmd(name: String): PushImageCmd = ???

  override def pushImageCmd(identifier: Identifier): PushImageCmd = ???

  override def inspectNetworkCmd(): InspectNetworkCmd = ???

  override def execCreateCmd(containerId: String): ExecCreateCmd = client.execCreateCmd(containerId)

  override def eventsCmd(): EventsCmd = ???

  override def renameContainerCmd(containerId: String): RenameContainerCmd = ???

  override def listImagesCmd(): ListImagesCmd = client.listImagesCmd()

  override def createContainerCmd(image: String): CreateContainerCmd = client.createContainerCmd(image)

  override def unpauseContainerCmd(containerId: String): UnpauseContainerCmd = ???

  override def buildImageCmd(): BuildImageCmd = ???

  override def buildImageCmd(dockerFileOrFolder: File): BuildImageCmd = ???

  override def buildImageCmd(tarInputStream: InputStream): BuildImageCmd = ???

  override def listVolumesCmd(): ListVolumesCmd = ???
}
