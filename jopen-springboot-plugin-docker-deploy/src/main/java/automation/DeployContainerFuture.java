package automation;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * @author maxuefeng
 * @see Operator
 */
@Getter
@Setter
@Deprecated
public class DeployContainerFuture {
    private Docker docker;
    private Hardware.MemoryInfo memoryInfo;
    private Hardware.DiskInfo diskInfo;
    private Set<Integer> bindedPorts;
}
