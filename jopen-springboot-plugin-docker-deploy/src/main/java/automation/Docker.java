package automation;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.jopen.springboot.plugin.common.IDUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;

/**
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
 *
 * @author maxuefeng
 */
@Getter
@Setter
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
//@Document(collection = "deploy_docker")
@Deprecated
public class Docker {

    @Id
    private String id;

    // docker image public ip
    private String publicIp;

    // docker image private ip
    private String privateIp;

    // repository
    private String repository;

    // size
    private String size;

    // docker container id
    private String containerId;

    // docker container name
    private String containerName;

    // image id
    private String imageId;

    // docker image name  such as vue
    private String imageName;

    // docker image tag(version) such 1.0
    private String tag;

    // docker out host mapping port
    private int outHostMappingPort;

    // docker inside host mapping port
    private int insideHostMappingPort = 80;

    // access docker image url
    private String accessDockerImageUrl;

    // create time
    private Long createTime;

    // docker created
    private String created;

    // ports
    private String ports;

    // command
    private String command;

    // status
    private String status;

    // hardware#getId()
    private String hardwareId;

    // 辅助字段
    private String type;

    // orgId
    private String orgId;

    public Docker() {
        this.id = IDUtil.id();
    }

    /**
     * @param imageName          must be setup
     * @param tag                must be setup
     * @param outHostMappingPort must be setup
     */
    public Docker(String imageName, String tag, int outHostMappingPort) {
        this.imageName = imageName;
        this.tag = tag;
        this.outHostMappingPort = outHostMappingPort;
        this.id = IDUtil.id();
    }
}
