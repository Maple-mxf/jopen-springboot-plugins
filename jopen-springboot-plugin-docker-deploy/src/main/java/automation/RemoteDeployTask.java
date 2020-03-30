package automation;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.onepushing.springboot.support.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.ExpectIOException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.sf.expectit.matcher.Matchers.contains;

/**
 * @author maxuefeng
 * @see Docker
 * <pre>
 *     docker
 *
 * Usage:	docker COMMAND
 *
 * A self-sufficient runtime for containers
 *
 * Options:
 *       --config string      Location of client config files (default "/root/.docker")
 *   -D, --debug              Enable debug mode
 *       --help               Print usage
 *   -H, --host list          Daemon socket(s) to connect to (default [])
 *   -l, --log-level string   Set the logging level ("debug", "info", "warn", "error", "fatal") (default "info")
 *       --tls                Use TLS; implied by --tlsverify
 *       --tlscacert string   Trust certs signed only by this CA (default "/root/.docker/ca.pem")
 *       --tlscert string     Path to TLS certificate file (default "/root/.docker/cert.pem")
 *       --tlskey string      Path to TLS key file (default "/root/.docker/key.pem")
 *       --tlsverify          Use TLS and verify the remote
 *   -v, --version            Print version information and quit
 *
 * Management Commands:
 *   container   Manage containers
 *   image       Manage images
 *   network     Manage networks
 *   node        Manage Swarm nodes
 *   plugin      Manage plugins
 *   secret      Manage Docker secrets
 *   service     Manage services
 *   stack       Manage Docker stacks
 *   swarm       Manage Swarm
 *   system      Manage Docker
 *   volume      Manage volumes
 *
 * Commands:
 *   attach      Attach to a running container
 *   build       Build an image from a Dockerfile
 *   commit      Create a new image from a container's changes
 *   cp          Copy files/folders between a container and the local filesystem
 *   create      Create a new container
 *   diff        Inspect changes on a container's filesystem
 *   events      Get real time events from the server
 *   exec        Run a command in a running container
 *   export      Export a container's filesystem as a tar archive
 *   history     Show the history of an image
 *   images      List images
 *   import      Import the contents from a tarball to create a filesystem image
 *   info        Display system-wide information
 *   inspect     Return low-level information on Docker objects
 *   kill        Kill one or more running containers
 *   load        Load an image from a tar archive or STDIN
 *   login       Log in to a Docker registry
 *   logout      Log out from a Docker registry
 *   logs        Fetch the logs of a container
 *   pause       Pause all processes within one or more containers
 *   port        List port mappings or a specific mapping for the container
 *   ps          List containers
 *   pull        Pull an image or a repository from a registry
 *   push        Push an image or a repository to a registry
 *   rename      Rename a container
 *   restart     Restart one or more containers
 *   rm          Remove one or more containers
 *   rmi         Remove one or more images
 *   run         Run a command in a new container
 *   save        Save one or more images to a tar archive (streamed to STDOUT by default)
 *   search      Search the Docker Hub for images
 *   start       Start one or more stopped containers
 *   stats       Display a live stream of container(s) resource usage statistics
 *   stop        Stop one or more running containers
 *   tag         Create a tag TARGET_IMAGE that refers to SOURCE_IMAGE
 *   top         Display the running processes of a container
 *   unpause     Unpause all processes within one or more containers
 *   update      Update configuration of one or more containers
 *   version     Show the Docker version information
 *   wait        Block until one or more containers stop, then print their exit codes
 * </pre>
 * @see Expect
 * @since 2020/3/19
 */
@Slf4j
@Deprecated
public class RemoteDeployTask implements Callable<DeployContainerFuture> {

    private final JSch jSch;

    private final com.jcraft.jsch.Session session;

    private final Channel channel;

    private final Expect shellCmdExpect;

    // remote deploy dir
    private final String targetBuildDir = "/usr/local/rdp";

    // target dir files
    @Deprecated
    private final String[] targetFiles = {"Dockerfile", "dist", "index.html"};


    // --------------------command---------------------------//
    // --------------------command---------------------------//
    // create target dir
    @Deprecated
    private final String createTargetDirCmd = "mkdir /usr/local/rdp";
    // view target dir all file
    private final String viewTargetDirCmd = String.format("ls %s", targetBuildDir);
    // checkup is install docker
    private final String checkupInstalledDockerCmd;
    // update yun source
    private final String updateCmd;
    // install docker
    private final String installDockerCmd;
    // install docker-ce
    private final String installDockerCeCmd;
    // cd build docker target dir
    private final String cdBuildDockerDirCmd;
    // docker tag (docker required repository name must be lowercase)
    private final String dockerTagCmd;
    // build docker image
    private final String buildDockerImageCmd;
    // port mapping such as -p 7001:80
    private final String portMappingCmd;
    // docker run -d -p
    private final String runDockerContainerCmd;
    // docker ps
    private final String runningContainerProcessCmd;
    // docker ps -a
    private final String allContainerCmd;
    // docker ps
    private final String allRunningContainerCmd;
    // systemctl status docker
    private final String checkupDockerServiceState;
    // systemctl start docker
    private final String startDockerService;

    // docker images
    private final String dockerImageListCmd;
    // docker rmi -f image id
    private final String rmiDockerImageCmd;
    //
    private final String rmDockerContainerCmd;
    // docker stop dockerImageId
    private final String stopDockerContainerCmd;
    // reload nginx
    private final String reloadNginxCmd;

    // docker image info
    private final Docker docker;

    // --------------------command---------------------------//
    // --------------------command---------------------------//

    private ReloadGatewayConfigPerformer reloadGatewayConfigPerformer;

    /**
     * @param adapter linux adapter info
     */
    public RemoteDeployTask(Adapter adapter,
                            Docker docker,
                            ExecutorService backgroundReadSSHStreamExecutor,
                            ReloadGatewayConfigPerformer reloadGatewayConfigPerformer) throws JSchException, IOException {
        Verify.verify(adapter != null && docker != null);

        this.jSch = new JSch();
        if (Adapter.AuthType.PRIVATE_KEY.equals(adapter.getAuthType())) {
            Verify.verify(adapter.getPrvKeyFile() != null);
            jSch.addIdentity(adapter.getPrvKeyFile());
            // jSch.addIdentity();
        }

        this.session = jSch.getSession(adapter.getUsername(), adapter.getHost(), adapter.getConnectPort());
        this.session.setPassword(adapter.getPassword());
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        this.session.setConfig(config);
        this.session.connect();
        this.channel = session.openChannel("shell");
        this.channel.connect();

        this.checkupInstalledDockerCmd = "docker";
        this.updateCmd = "yum -y update";
        this.installDockerCmd = "yum -y install docker";
        this.installDockerCeCmd = "yum -y install docker-ce";
        this.cdBuildDockerDirCmd = String.format("cd %s", targetBuildDir);
        this.dockerTagCmd = String.format("%s:%s", docker.getImageName(), docker.getTag());
        this.buildDockerImageCmd = String.format("docker build -t %s .", dockerTagCmd);
        this.portMappingCmd = String.format("%s:%s", docker.getOutHostMappingPort(), docker.getInsideHostMappingPort());
        this.runDockerContainerCmd = String.format("docker run -d -p %s --name %s   %s",
                // port mapping
                portMappingCmd,
                // setup docker image name
                docker.getImageName(),
                // setup docker image tag
                dockerTagCmd);
        //
        this.runningContainerProcessCmd = String.format("docker ps --filter name=%s", docker.getImageName());
        this.dockerImageListCmd = "docker images";

        // {@code this#wrapRmiDockerImageCmd}
        // {@code this#wrapStopDockerImageCmd}
        this.rmiDockerImageCmd = "docker rmi -f";
        this.stopDockerContainerCmd = "docker stop ";
        this.rmDockerContainerCmd = "docker rm -f";
        this.allContainerCmd = "docker ps -a";
        this.allRunningContainerCmd = "docker ps";
        this.checkupDockerServiceState = "systemctl status docker";
        this.startDockerService = "systemctl start docker";
        this.reloadNginxCmd = "./sbin/nginx -s reload";

        this.docker = docker;
        this.docker.setPublicIp(adapter.getHost());
        this.docker.setContainerName(docker.getImageName());
        this.docker.setAccessDockerImageUrl(String.format("http://%s:%s", adapter.getHost(), docker.getOutHostMappingPort()));

        this.shellCmdExpect = new ExpectBuilder()
                .withOutput(channel.getOutputStream())
                .withInputs(channel.getInputStream(), channel.getExtInputStream())
                .withEchoOutput(System.out)
                .withCharset(StandardCharsets.UTF_8)
                .withEchoInput(System.err)
                .withExecutor(backgroundReadSSHStreamExecutor)
                .withExceptionOnFailure()
                .withAutoFlushEcho(true)
                .build();

        this.reloadGatewayConfigPerformer = reloadGatewayConfigPerformer;
    }

    private String wrapRmiImageCmd(String imageId) {
        return String.join(" ", this.rmiDockerImageCmd, imageId);
    }

    private String wrapRmContainerCmd(String containerId) {
        return String.join(" ", this.rmDockerContainerCmd, containerId);
    }

    private String wrapStopContainerCmd(String containerId) {
        return String.join(" ", this.stopDockerContainerCmd, containerId);
    }

    // previous checkup
    private void checkupIsInstalledDocker() throws IOException {
        try {
            this.shellCmdExpect.sendLine(this.checkupInstalledDockerCmd);
            this.shellCmdExpect
                    .withTimeout(2, TimeUnit.SECONDS)
                    .expect(contains("Usage")).isSuccessful();
        } catch (ExpectIOException ignored) {
            this.installDocker();
        }
    }

    private void checkupIsStartedDocker() throws IOException {
        try {
            this.shellCmdExpect.sendLine(this.checkupDockerServiceState);
            String echo = this.shellCmdExpect
                    .withTimeout(3, TimeUnit.SECONDS)
                    .expect(contains("Active"))
                    .getInput();
            String[] lines = echo.split("\n");
            boolean running = false;
            for (String line : lines) {
                if (line.contains("Active") && line.contains("running")) {
                    running = true;
                    break;
                }
            }
            if (!running) {
                try {
                    this.shellCmdExpect.sendLine(this.startDockerService)
                            .withTimeout(20, TimeUnit.SECONDS);
                } catch (Throwable throwable) {
                    throw new ServiceException(throwable.getMessage());
                }
            }
        } catch (ExpectIOException ignored) {
        }
    }

    // install docker
    private void installDocker() throws IOException {
        try {
            this.shellCmdExpect.sendLine(this.installDockerCmd);
            this.shellCmdExpect
                    .withTimeout(120, TimeUnit.SECONDS)
                    .expect(contains("Complete!")).isSuccessful();
        } catch (ExpectIOException ignored) {
            throw new ServiceException("deploy failure install docker fail");
        }
    }

    // cd target dir
    private void cdTargetDir() throws IOException {
        try {
            this.shellCmdExpect
                    .withTimeout(2, TimeUnit.SECONDS)
                    .sendLine(this.cdBuildDockerDirCmd);
        } catch (ExpectIOException e) {
            throw new ServiceException(String.format("cd target dir error %s", e.getMessage()));
        }
        this.shellCmdExpect.sendLine(this.viewTargetDirCmd);
        String echo = this.shellCmdExpect
                .withTimeout(3, TimeUnit.SECONDS)
                .expect(contains("Dockerfile"))
                .getInput();
        for (String targetFileName : this.targetFiles) {
            if (!echo.contains(targetFileName))
                throw new ServiceException(String.format("target dir %s has not target file", Joiner.on(", ").join(this.targetFiles)));
        }
    }

    private void buildDockerImage() throws IOException {
        String input = null;
        try {
            this.shellCmdExpect.sendLine(this.buildDockerImageCmd);
            input = this.shellCmdExpect
                    .withTimeout(300, TimeUnit.SECONDS)
                    .expect(contains("Successfully built"))
                    .getInput();
        } catch (ExpectIOException ignored) {
            throw new ServiceException(input);
        }
        if (!isBuildDockerSuccess()) {
            throw new ServiceException("build docker error");
        }
    }

    private void runDockerImage() throws IOException {
        String input = null;
        try {
            this.shellCmdExpect.sendLine(this.runDockerContainerCmd);
            input = this.shellCmdExpect.expect(contains(this.dockerTagCmd)).getInput();
        } catch (ExpectIOException ignored) {
            throw new ServiceException(input);
        }

    }

    private void checkupDockerImageProcess() throws IOException {
        try {
            this.shellCmdExpect.sendLine(this.allRunningContainerCmd);
            String echo = this.shellCmdExpect
                    .withTimeout(3, TimeUnit.SECONDS)
                    .expect(contains("CONTAINER ID"))
                    .getInput();

            List<Docker> dockers = this.mapperDockerContainer(echo);
            Docker tmpDocker = dockers.stream()
                    .filter(t -> this.docker.getImageName().equals(t.getContainerName()))
                    .findFirst()
                    .orElse(null);

            if (tmpDocker != null) {
                this.docker.setContainerId(tmpDocker.getContainerId());
                this.docker.setCommand(tmpDocker.getCommand());
                this.docker.setPorts(tmpDocker.getPorts());
                this.docker.setStatus(tmpDocker.getStatus());
                this.docker.setCreated(tmpDocker.getCreated());
            }

        } catch (ExpectIOException ignored) {
            throw new ServiceException("docker build success but not docker image process");
        }
    }

    private boolean isBuildDockerSuccess() throws IOException {
        this.shellCmdExpect.sendLine(this.dockerImageListCmd);
        String echo = this.shellCmdExpect
                .withTimeout(3, TimeUnit.SECONDS)
                .expect(contains("REPOSITORY")).getInput();

        List<Docker> dockers = mapperDockerImages(echo);
        Docker tmpDocker = dockers.stream()
                .filter(t -> t.getRepository().trim().equals(this.docker.getImageName()))
                .findFirst()
                .orElse(null);

        if (tmpDocker != null) {
            this.docker.setSize(tmpDocker.getSize());
            this.docker.setImageId(tmpDocker.getImageId());
            this.docker.setRepository(tmpDocker.getRepository());
            return true;
        }
        return false;
    }

    /**
     * @see Docker
     * <pre>
     *     REPOSITORY TAG IMAGE ID CREATED  SIZE
     * </pre>
     */
    private List<Docker> mapperDockerImages(String echo) {
        if (echo == null || echo.length() == 0) return Lists.newArrayList();
        echo = echo.replaceAll("\r", "");
        String[] lineArr = echo.split("\n");
        if (lineArr.length == 0 || lineArr.length == 1) return Lists.newArrayList();

        int colIndex = 0;
        for (int i = 0; i < lineArr.length; i++) {
            if (lineArr[i].contains("REPOSITORY")
                    && lineArr[i].contains("TAG")
                    && lineArr[i].contains("IMAGE ID")
                    && lineArr[i].contains("CREATED")
                    && lineArr[i].contains("SIZE")
            ) {
                colIndex = i;
                break;
            }
        }

        if (colIndex + 1 != lineArr.length) {
            int tagColIndex = lineArr[colIndex].indexOf("TAG");
            int imageIdColIndex = lineArr[colIndex].indexOf("IMAGE ID");
            int createdColIndex = lineArr[colIndex].indexOf("CREATED");
            int sizeColIndex = lineArr[colIndex].indexOf("SIZE");

            List<Docker> dockers = new ArrayList<>();

            for (int i = colIndex + 1; i < lineArr.length; i++) {
                String line = lineArr[i];
                if (StringUtils.isBlank(line)) continue;
                try {
                    String repository = line.substring(0, tagColIndex);
                    String tag = line.substring(tagColIndex, imageIdColIndex);
                    String imageId = line.substring(imageIdColIndex, createdColIndex);
                    String created = line.substring(createdColIndex, sizeColIndex);
                    String size = line.substring(sizeColIndex);

                    Docker docker = new Docker();
                    docker.setRepository(repository);
                    docker.setTag(tag);
                    docker.setImageId(imageId);
                    docker.setCreated(created);
                    docker.setSize(size);

                    dockers.add(docker);
                } catch (Throwable ignored) {
                }
            }
            return dockers;
        } else {
            return Lists.newArrayList();
        }
    }

    /**
     * filter running docker container
     */
    private List<Docker> getRunningDockerContainers() throws IOException {
        this.shellCmdExpect.sendLine(this.allRunningContainerCmd);
        String echo = this.shellCmdExpect
                .withTimeout(5, TimeUnit.SECONDS)
                .expect(contains("CONTAINER ID")).getInput();
        return mapperDockerContainer(echo);
    }

    /**
     * all docker container
     */
    private List<Docker> getAllDockerContainers() throws IOException {
        try {
            this.shellCmdExpect.sendLine(this.allContainerCmd);
            String echo = this.shellCmdExpect
                    .withTimeout(5, TimeUnit.SECONDS)
                    .expect(contains("CONTAINER ID")).getInput();
            return mapperDockerContainer(echo);
        } catch (ExpectIOException ignored) {
            throw new RuntimeException("docker images command error");
        }
    }

    /**
     * <pre>
     *     CONTAINER ID IMAGE COMMAND CREATED STATUS PORTS NAMES
     * </pre>
     *
     * @param echo
     * @return
     */
    private List<Docker> mapperDockerContainer(String echo) {
        if (echo == null || echo.length() == 0) return Lists.newArrayList();
        echo = echo.replaceAll("\r", "");
        String[] lineArr = echo.split("\n");
        if (lineArr.length == 0 || lineArr.length == 1) return Lists.newArrayList();

        int colIndex = 0;
        for (int i = 0; i < lineArr.length; i++) {
            if (lineArr[i].contains("CONTAINER ID")
                    && lineArr[i].contains("IMAGE")
                    && lineArr[i].contains("COMMAND")
                    && lineArr[i].contains("CREATED")
                    && lineArr[i].contains("STATUS")
                    && lineArr[i].contains("PORTS")
                    && lineArr[i].contains("NAMES")
            ) {
                colIndex = i;
                break;
            }
        }

        if (colIndex + 1 != lineArr.length) {
            int imageColIndex = lineArr[colIndex].indexOf("IMAGE");
            int commandColIndex = lineArr[colIndex].indexOf("COMMAND");
            int createdColIndex = lineArr[colIndex].indexOf("CREATED");
            int statusColIndex = lineArr[colIndex].indexOf("STATUS");
            int portsColIndex = lineArr[colIndex].indexOf("PORTS");
            int namesColIndex = lineArr[colIndex].indexOf("NAMES");

            List<Docker> dockers = new ArrayList<>();

            for (int i = colIndex + 1; i < lineArr.length; i++) {
                String line = lineArr[i];
                if (StringUtils.isBlank(line)) continue;
                try {
                    String containerId = line.substring(0, imageColIndex);
                    String image = line.substring(imageColIndex, commandColIndex);
                    String command = line.substring(commandColIndex, createdColIndex);
                    String created = line.substring(createdColIndex, statusColIndex);
                    String status = line.substring(statusColIndex, portsColIndex);
                    String ports = line.substring(portsColIndex, namesColIndex);
                    String names = line.substring(namesColIndex);

                    Docker docker = new Docker();
                    docker.setContainerId(containerId.trim());
                    docker.setImageName(image.trim());
                    docker.setCommand(command.trim());
                    docker.setCreated(created);
                    docker.setPorts(ports);
                    docker.setStatus(status);
                    docker.setContainerName(names);

                    dockers.add(docker);
                } catch (Throwable ignored) {
                }
            }
            return dockers;
        } else {
            return Lists.newArrayList();
        }
    }

    private void releaseSource() throws IOException {
        log.info("release ssh source");
        this.shellCmdExpect.close();
        this.channel.disconnect();
        this.session.disconnect();
    }

    private void stopContainer(String containerId) throws IOException {
        try {
            this.shellCmdExpect.sendLine(this.wrapStopContainerCmd(containerId));
            this.shellCmdExpect
                    .withTimeout(5, TimeUnit.SECONDS)
                    .expect(contains(containerId));
        } catch (ExpectIOException ignored) {
        }
    }

    private void rmContainer(String containerId) throws IOException {
        try {
            if (this.getAllDockerContainers().stream().anyMatch(t -> t.getContainerId().equals(containerId))) {
                this.shellCmdExpect.sendLine(this.wrapRmContainerCmd(containerId));
                this.shellCmdExpect
                        .withTimeout(5, TimeUnit.SECONDS)
                        .expect(contains(containerId));
            }
        } catch (ExpectIOException ignored) {
        }
    }

    private void rmiImage(String imageId) throws IOException {
        try {
            this.shellCmdExpect.sendLine(this.wrapRmiImageCmd(imageId));
            this.shellCmdExpect
                    .withTimeout(5, TimeUnit.SECONDS)
                    .expect(contains(imageId));
        } catch (ExpectIOException ignored) {
        }
    }

    private void reloadNginx() throws IOException {
        reloadGatewayConfigPerformer.reload(this.docker);
        this.shellCmdExpect.sendLine(this.reloadNginxCmd);
    }

    @Override
    public DeployContainerFuture call() throws Exception {
        // 0
        beforeDeploy();

        // 1 cd
        cdTargetDir();

        // 2 build docker
        buildDockerImage();

        // 3 run docker
        runDockerImage();

        // 4 checkup docker process
        checkupDockerImageProcess();

        // 5 reload nginx getway
        reloadNginx();

        this.docker.setRepository(this.docker.getImageName());
        this.docker.setCreateTime(new Date().getTime());

        mapperDockerContainer(this.dockerImageListCmd).stream()
                .filter(t -> t.getRepository().equals(this.docker.getImageName()))
                .findFirst()
                .ifPresent(t -> this.docker.setImageId(t.getImageId()));

        // 获取当前服务器实例的信息
        Operator operator = new Operator(this.shellCmdExpect);
        Set<Integer> bindedPorts = operator.getBindedPorts();
        Hardware.DiskInfo diskInfo = operator.getDiskInfo();
        Hardware.MemoryInfo memoryInfo = operator.getMemoryInfo();

        DeployContainerFuture deployContainerFuture = new DeployContainerFuture();
        deployContainerFuture.setDocker(this.docker);
        deployContainerFuture.setDiskInfo(diskInfo);
        deployContainerFuture.setBindedPorts(bindedPorts);
        deployContainerFuture.setMemoryInfo(memoryInfo);

        // release source
        releaseSource();

        return deployContainerFuture;
    }

    /**
     * before deploy delete docker container and docker images
     */
    private void beforeDeploy() throws IOException {

        // 1 checkup is installed docker
        checkupIsInstalledDocker();

        // 2 checkup is started docker
        checkupIsStartedDocker();

        // 3 remove will be deploy docker container
        List<Docker> runningDockers = getRunningDockerContainers();

        Set<String> containerIds = runningDockers.stream()
                .filter(t -> t.getContainerName().equals(this.docker.getContainerName()))
                .map(Docker::getContainerId)
                .collect(Collectors.toSet());
        for (String containerId : containerIds) {
            // stop running docker container
            stopContainer(containerId);
            // rm -f docker container
            rmContainer(containerId);
        }

        // 4 remove will be deploy docker image
        this.shellCmdExpect.sendLine(this.dockerImageListCmd);
        String echo = this.shellCmdExpect
                .withTimeout(3, TimeUnit.SECONDS)
                .expect(contains("REPOSITORY"))
                .getInput();

        List<Docker> dockerImages = mapperDockerImages(echo);
        List<String> imageIds = dockerImages.stream()
                .filter(t -> {
                    if (StringUtils.isBlank(t.getRepository()) || StringUtils.isBlank(t.getTag())) return false;
                    t.setRepository(t.getRepository().trim());
                    t.setTag(t.getTag().trim());
                    return t.getRepository().equals(this.docker.getImageName()) && this.docker.getTag().equals(t.getTag());
                })
                .map(Docker::getImageId)
                .collect(Collectors.toList());
        for (String imageId : imageIds) {
            // rmi -f docker image
            rmiImage(imageId);
        }
    }
}