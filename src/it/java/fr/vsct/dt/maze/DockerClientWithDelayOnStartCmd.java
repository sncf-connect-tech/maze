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
 */

package fr.vsct.dt.maze;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Identifier;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class DockerClientWithDelayOnStartCmd implements DockerClient {

    private final DockerClient client;
    private final int delay;

    public DockerClientWithDelayOnStartCmd(DockerClient client, int delay) {
        this.client = client;
        this.delay = delay;
    }

    @Override
    public AuthConfig authConfig() throws DockerException {
        throw new NotImplementedException();
    }

    @Override
    public AuthCmd authCmd() {
        throw new NotImplementedException();
    }

    @Override
    public InfoCmd infoCmd() {
        throw new NotImplementedException();
    }

    @Override
    public PingCmd pingCmd() {
        throw new NotImplementedException();
    }

    @Override
    public VersionCmd versionCmd() {
        throw new NotImplementedException();
    }

    @Override
    public PullImageCmd pullImageCmd(@Nonnull String repository) {
        throw new NotImplementedException();
    }

    @Override
    public PushImageCmd pushImageCmd(@Nonnull String name) {
        throw new NotImplementedException();
    }

    @Override
    public PushImageCmd pushImageCmd(@Nonnull Identifier identifier) {
        throw new NotImplementedException();
    }

    @Override
    public CreateImageCmd createImageCmd(@Nonnull String repository, @Nonnull InputStream imageStream) {
        throw new NotImplementedException();
    }

    @Override
    public LoadImageCmd loadImageCmd(@Nonnull InputStream imageStream) {
        throw new NotImplementedException();
    }

    @Override
    public SearchImagesCmd searchImagesCmd(@Nonnull String term) {
        throw new NotImplementedException();
    }

    @Override
    public RemoveImageCmd removeImageCmd(@Nonnull String imageId) {
        throw new NotImplementedException();
    }

    @Override
    public ListImagesCmd listImagesCmd() {
        throw new NotImplementedException();
    }

    @Override
    public InspectImageCmd inspectImageCmd(@Nonnull String imageId) {
        throw new NotImplementedException();
    }

    @Override
    public SaveImageCmd saveImageCmd(@Nonnull String name) {
        throw new NotImplementedException();
    }

    @Override
    public ListContainersCmd listContainersCmd() {
        throw new NotImplementedException();
    }

    @Override
    public CreateContainerCmd createContainerCmd(@Nonnull String image) {
        throw new NotImplementedException();
    }

    @Override
    public StartContainerCmd startContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public ExecCreateCmd execCreateCmd(@Nonnull String containerId) {
        return client.execCreateCmd(containerId);
    }

    @Override
    public InspectContainerCmd inspectContainerCmd(@Nonnull String containerId) {
        return client.inspectContainerCmd(containerId);
    }

    @Override
    public RemoveContainerCmd removeContainerCmd(@Nonnull String containerId) {
        return client.removeContainerCmd(containerId);
    }

    @Override
    public WaitContainerCmd waitContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public AttachContainerCmd attachContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public ExecStartCmd execStartCmd(@Nonnull String execId) {
        ExecStartCmd command = client.execStartCmd(execId);

        return new ExecStartCmd() {
            @Override
            public String getExecId() {
                return command.getExecId();
            }

            @Override
            public Boolean hasDetachEnabled() {
                return command.hasDetachEnabled();
            }

            @Override
            public Boolean hasTtyEnabled() {
                return command.hasTtyEnabled();
            }

            @Override
            public InputStream getStdin() {
                return command.getStdin();
            }

            @Override
            public ExecStartCmd withDetach(Boolean detach) {
                return command.withDetach(detach);
            }

            @Override
            public ExecStartCmd withExecId(@Nonnull String execId) {
                return command.withExecId(execId);
            }

            @Override
            public ExecStartCmd withTty(Boolean tty) {
                return command.withTty(tty);
            }

            @Override
            public ExecStartCmd withStdIn(InputStream stdin) {
                return command.withStdIn(stdin);
            }

            @Override
            public <T extends ResultCallback<Frame>> T exec(T resultCallback) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        command.exec(resultCallback);
                    }
                }, delay);
                return resultCallback;
            }

            @Override
            public void close() {
                command.close();
            }
        };
    }

    @Override
    public InspectExecCmd inspectExecCmd(@Nonnull String execId) {
        return client.inspectExecCmd(execId);
    }

    @Override
    public LogContainerCmd logContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public CopyArchiveFromContainerCmd copyArchiveFromContainerCmd(@Nonnull String containerId, @Nonnull String resource) {
        throw new NotImplementedException();
    }

    @Override
    public CopyFileFromContainerCmd copyFileFromContainerCmd(@Nonnull String containerId, @Nonnull String resource) {
        throw new NotImplementedException();
    }

    @Override
    public CopyArchiveToContainerCmd copyArchiveToContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public ContainerDiffCmd containerDiffCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public StopContainerCmd stopContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public KillContainerCmd killContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public UpdateContainerCmd updateContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public RenameContainerCmd renameContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public RestartContainerCmd restartContainerCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public CommitCmd commitCmd(@Nonnull String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public BuildImageCmd buildImageCmd() {
        throw new NotImplementedException();
    }

    @Override
    public BuildImageCmd buildImageCmd(File dockerFileOrFolder) {
        throw new NotImplementedException();
    }

    @Override
    public BuildImageCmd buildImageCmd(InputStream tarInputStream) {
        throw new NotImplementedException();
    }

    @Override
    public TopContainerCmd topContainerCmd(String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public TagImageCmd tagImageCmd(String imageId, String repository, String tag) {
        throw new NotImplementedException();
    }

    @Override
    public PauseContainerCmd pauseContainerCmd(String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public UnpauseContainerCmd unpauseContainerCmd(String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public EventsCmd eventsCmd() {
        throw new NotImplementedException();
    }

    @Override
    public StatsCmd statsCmd(String containerId) {
        throw new NotImplementedException();
    }

    @Override
    public CreateVolumeCmd createVolumeCmd() {
        throw new NotImplementedException();
    }

    @Override
    public InspectVolumeCmd inspectVolumeCmd(String name) {
        throw new NotImplementedException();
    }

    @Override
    public RemoveVolumeCmd removeVolumeCmd(String name) {
        throw new NotImplementedException();
    }

    @Override
    public ListVolumesCmd listVolumesCmd() {
        throw new NotImplementedException();
    }

    @Override
    public ListNetworksCmd listNetworksCmd() {
        throw new NotImplementedException();
    }

    @Override
    public InspectNetworkCmd inspectNetworkCmd() {
        throw new NotImplementedException();
    }

    @Override
    public CreateNetworkCmd createNetworkCmd() {
        throw new NotImplementedException();
    }

    @Override
    public RemoveNetworkCmd removeNetworkCmd(@Nonnull String networkId) {
        throw new NotImplementedException();
    }

    @Override
    public ConnectToNetworkCmd connectToNetworkCmd() {
        throw new NotImplementedException();
    }

    @Override
    public DisconnectFromNetworkCmd disconnectFromNetworkCmd() {
        throw new NotImplementedException();
    }

    @Override
    public void close() throws IOException {
        throw new NotImplementedException();
    }
}
