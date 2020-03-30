package automation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.onepushing.springboot.support.exception.ServiceException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 机器的硬件资源信息
 */
@Getter
@Setter
@ToString
//@Document(collection = "hardware")
@Deprecated
public class Hardware {

    @Transient
    public static transient int minPort = 1024;
    @Transient
    public static transient int maxPort = 65535;

    // 内存最小限制 单位为MB 如果小于100MB 则会将此服务器实例改变状态
    // 具体请看broken字段（防止宕机）
    @Transient
    private static transient float minMemoryLimit = 300f;

    @Id
    private String id;

    // CPU cores
    private int cpuCore;

    // publicIp
    private String publicIp;

    // privateIp
    private String privateIp;

    // unit MB
    private MemoryInfo memoryInfo;

    // unit MB
    private DiskInfo diskInfo;

    // server region
    private String region;

    // hardware account info
    private List<Adapter> adapters;

    // is avalible
    private Boolean broken;

    // update time
    private Long updateTime;

    // used ports
    private Set<Integer> usedPorts = new HashSet<>();
    // opened ports
    private Set<Integer> openedPorts = new HashSet<>();

    @Getter
    @Setter
    public static class MemoryInfo {
        private Float totalMemoryCap;
        private Float freeMemoryCap;
        private Float usedCap;
    }

    @Getter
    @Setter
    public static class DiskInfo {
        private Float totalDiskCap;
        private Float freeDiskCap;
        private Float usedCap;
    }

    @JsonIgnore
    public int randomPort() {
        int port;
        while (!this.usedPorts.contains(port = RandomUtils.nextInt(minPort + 1, maxPort - 1)))
            return port;
        throw new ServiceException("找不到合适的端口");
    }

}
