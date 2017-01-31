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
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public AuthCmd authCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public InfoCmd infoCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public PingCmd pingCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public VersionCmd versionCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public PullImageCmd pullImageCmd(@Nonnull String repository) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public PushImageCmd pushImageCmd(@Nonnull String name) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public PushImageCmd pushImageCmd(@Nonnull Identifier identifier) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public CreateImageCmd createImageCmd(@Nonnull String repository, @Nonnull InputStream imageStream) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public LoadImageCmd loadImageCmd(@Nonnull InputStream imageStream) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public SearchImagesCmd searchImagesCmd(@Nonnull String term) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public RemoveImageCmd removeImageCmd(@Nonnull String imageId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public ListImagesCmd listImagesCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public InspectImageCmd inspectImageCmd(@Nonnull String imageId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public SaveImageCmd saveImageCmd(@Nonnull String name) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public ListContainersCmd listContainersCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public CreateContainerCmd createContainerCmd(@Nonnull String image) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public StartContainerCmd startContainerCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
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
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public AttachContainerCmd attachContainerCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
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
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public CopyArchiveFromContainerCmd copyArchiveFromContainerCmd(@Nonnull String containerId, @Nonnull String resource) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public CopyFileFromContainerCmd copyFileFromContainerCmd(@Nonnull String containerId, @Nonnull String resource) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public CopyArchiveToContainerCmd copyArchiveToContainerCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public ContainerDiffCmd containerDiffCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public StopContainerCmd stopContainerCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public KillContainerCmd killContainerCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public UpdateContainerCmd updateContainerCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public RenameContainerCmd renameContainerCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public RestartContainerCmd restartContainerCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public CommitCmd commitCmd(@Nonnull String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public BuildImageCmd buildImageCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public BuildImageCmd buildImageCmd(File dockerFileOrFolder) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public BuildImageCmd buildImageCmd(InputStream tarInputStream) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public TopContainerCmd topContainerCmd(String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public TagImageCmd tagImageCmd(String imageId, String repository, String tag) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public PauseContainerCmd pauseContainerCmd(String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public UnpauseContainerCmd unpauseContainerCmd(String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public EventsCmd eventsCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public StatsCmd statsCmd(String containerId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public CreateVolumeCmd createVolumeCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public InspectVolumeCmd inspectVolumeCmd(String name) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public RemoveVolumeCmd removeVolumeCmd(String name) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public ListVolumesCmd listVolumesCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public ListNetworksCmd listNetworksCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public InspectNetworkCmd inspectNetworkCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public CreateNetworkCmd createNetworkCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public RemoveNetworkCmd removeNetworkCmd(@Nonnull String networkId) {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public ConnectToNetworkCmd connectToNetworkCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public DisconnectFromNetworkCmd disconnectFromNetworkCmd() {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }

    @Override
    public void close() throws IOException {
        throw new RuntimeException("Implement this method with a mock behavior if needed");
    }
}
