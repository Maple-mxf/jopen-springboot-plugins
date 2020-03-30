package automation;

import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.onepushing.springboot.support.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectIOException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static net.sf.expectit.matcher.Matchers.contains;

/**
 * @author maxuefeng
 */
@Slf4j
@Deprecated
public class Operator {

    //
    private final Expect shellCmdExpect;

    // docker ps -a
    private final String allContainerCmd;
    // docker image list
    private final String dockerImageListCmd;
    // netstat -ntlp
    private final String netsatBindPorts;
    // free -m
    private final String freeMem;
    // df -hl
    private final String freeDisk;

    public Operator(Expect shellCmdExpect) {
        Verify.verify(shellCmdExpect != null, "shellCmdExpect参数不可为空");
        this.shellCmdExpect = shellCmdExpect;

        // command
        this.allContainerCmd = "docker ps -a";
        this.dockerImageListCmd = "docker images";
        this.netsatBindPorts = "netstat -ntlp";
        this.freeMem = "free -m";
        this.freeDisk = "df -h";
    }

    public Set<Integer> getBindedPorts() throws IOException {
        this.shellCmdExpect.sendLine(this.netsatBindPorts);
        try {
            String echo = this.shellCmdExpect
                    .withTimeout(3, TimeUnit.SECONDS)
                    .expect(contains("tcp")).getInput();
            String[] lines = echo.split("\n");
            Pattern pattern = Pattern.compile(".*[:]\\d{4,5}");
            Set<Integer> bindedPorts = new HashSet<>();

            Stream.of(lines)
                    .filter(line -> StringUtils.isNotBlank(line) && line.startsWith("tcp"))
                    .forEach(line -> {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String[] arr = matcher.group().split(":");
                            try {
                                if (arr.length > 0) bindedPorts.add(Integer.parseInt(arr[arr.length - 1]));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
            return bindedPorts;
        } catch (ExpectIOException e) {
            log.error("regex bind port failure  {} ", e.getMessage());
        }
        return null;
    }

    public Hardware.MemoryInfo getMemoryInfo() throws IOException {
        this.shellCmdExpect.sendLine(this.freeMem);
        try {
            String echo = this.shellCmdExpect
                    .withTimeout(3, TimeUnit.SECONDS)
                    .expect(contains("Mem")).getInput();
            String string = Stream.of(echo.split("\n")).filter(line -> line.startsWith("Mem:")).findFirst().orElse(null);
            if (string == null) throw new ServiceException("free -m command execute failure");

            String[] arr = string.split("\\W+");
            float totalMem = Float.parseFloat(arr[1].trim());
            float usedMem = Float.parseFloat(arr[2].trim());

            Hardware.MemoryInfo memoryInfo = new Hardware.MemoryInfo();
            memoryInfo.setTotalMemoryCap(totalMem);
            memoryInfo.setUsedCap(usedMem);
            memoryInfo.setFreeMemoryCap(totalMem - usedMem);

            return memoryInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return new Hardware.MemoryInfo();
        }
    }

    /**
     * <pre>
     * Filesystem      Size  Used Avail Use% Mounted on
     * devtmpfs        3.7G     0  3.7G   0% /dev
     * tmpfs           3.7G  8.0K  3.7G   1% /dev/shm
     * tmpfs           3.7G  832K  3.7G   1% /run
     * tmpfs           3.7G     0  3.7G   0% /sys/fs/cgroup
     * /dev/vda1        63G  8.8G   52G  15% /
     * tmpfs           756M     0  756M   0% /run/user/0
     * overlay          63G  8.8G   52G  15% /var/lib/docker/overlay2/f5f311788959b9bdfbf421a1b199387171b472be56b2078c9d2ff4517a8858ad/merged
     * shm              64M     0   64M   0% /var/lib/docker/containers/b69971fc48be892c7b49207624d5fb29691d2710e49d39529e1446315b03ac6e/shm
     * overlay          63G  8.8G   52G  15% /var/lib/docker/overlay2/07a1e94e92fdb9acdbd1c271bf217ec99d5d98fcf20888000e0c0e4eb982374f/merged
     * shm              64M     0   64M   0% /var/lib/docker/containers/eeec56ed8068b630b05c176f7bdb121a49d43e43e3bf0179ff3d7fe121be8e27/shm
     * </pre>
     */
    public Hardware.DiskInfo getDiskInfo() throws IOException {
        try {
            this.shellCmdExpect.sendLine(this.freeDisk);
            String echo = this.shellCmdExpect
                    .withTimeout(3, TimeUnit.SECONDS)
                    .expect(contains("Filesystem"))
                    .getInput();

            return mapperDiskInfo(echo);
        } catch (ExpectIOException e) {
            e.printStackTrace();
        }
        return new Hardware.DiskInfo();
    }

    // Filesystem      Size  Used Avail Use% Mounted on
    private Hardware.DiskInfo mapperDiskInfo(String echo) {
        if (echo == null || echo.length() == 0) return new Hardware.DiskInfo();
        echo = echo.replaceAll("\r", "");
        String[] lineArr = echo.split("\n");
        if (lineArr.length == 0 || lineArr.length == 1) return new Hardware.DiskInfo();

        int colIndex = 0;
        for (int i = 0; i < lineArr.length; i++) {
            if (lineArr[i].contains("Filesystem")
                    && lineArr[i].contains("Size")
                    && lineArr[i].contains("Used")
                    && lineArr[i].contains("Avail")
            ) {
                colIndex = i;
                break;
            }
        }
        if (colIndex + 1 != lineArr.length) {
            int sizeColIndex = lineArr[colIndex].indexOf("Size");
            int usedColIndex = lineArr[colIndex].indexOf("Used");
            int availColIndex = lineArr[colIndex].indexOf("Avail");

            Pattern pattern = Pattern.compile("^\\d?[.]?\\d?[MGK]");

            float diskTotalSize = 0f;
            float diskUsedSize = 0f;

            for (int i = colIndex + 1; i < lineArr.length; i++) {
                String line = lineArr[i];
                if (StringUtils.isBlank(line)) continue;
                try {
                    String size = line.substring(sizeColIndex, usedColIndex).trim();
                    String used = line.substring(usedColIndex, availColIndex).trim();

                    Matcher matchSize = pattern.matcher(size);
                    Matcher matchUsed = pattern.matcher(used);
                    if (matchSize.find() && ("0".equals(used) || matchUsed.find())) {
                        String sizeStr = matchSize.group();
                        float tmpSize = -1f;
                        if (sizeStr.endsWith("G")) {
                            tmpSize = Float.parseFloat(sizeStr.replaceAll("G", "")) * 1024f;
                        } else if (sizeStr.endsWith("M")) {
                            tmpSize = Float.parseFloat(sizeStr.replaceAll("M", ""));
                        } else if (sizeStr.endsWith("K")) {
                            tmpSize = Float.parseFloat(sizeStr.replaceAll("K", "")) / 1024f;
                        }

                        if (tmpSize == -1f) continue;
                        String usedStr = "0".equals(used) ? "0" : matchUsed.group();

                        float tmpUsed = -1f;
                        if (usedStr.endsWith("G")) tmpUsed = Float.parseFloat(sizeStr.replaceAll("G", "")) * 1024f;
                        else if (usedStr.endsWith("M")) tmpUsed = Float.parseFloat(sizeStr.replaceAll("M", ""));
                        else if (usedStr.endsWith("K")) tmpUsed = Float.parseFloat(sizeStr.replaceAll("K", "")) / 1024f;
                        else if (usedStr.equals("0"))
                            tmpUsed = 0f;

                        if (tmpUsed == -1f) continue;
                        diskTotalSize = diskTotalSize + tmpSize;
                        diskUsedSize = diskUsedSize + tmpUsed;
                    }


                } catch (Throwable ignored) {
                }
            }
            Hardware.DiskInfo diskInfo = new Hardware.DiskInfo();
            diskInfo.setTotalDiskCap(diskTotalSize);
            diskInfo.setUsedCap(diskUsedSize);
            diskInfo.setFreeDiskCap(diskTotalSize - diskUsedSize);
            return diskInfo;
        }
        return new Hardware.DiskInfo();
    }


    /**
     * all docker container
     */
    public List<Docker> getAllDockerContainers() throws IOException {
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

    public List<Docker> getAllDockerImages() throws IOException {
        this.shellCmdExpect.sendLine(this.dockerImageListCmd);
        String echo = this.shellCmdExpect
                .withTimeout(3, TimeUnit.SECONDS)
                .expect(contains("REPOSITORY")).getInput();
        return mapperDockerImages(echo);
    }

    /**
     * <pre>
     *     CONTAINER ID IMAGE COMMAND CREATED STATUS PORTS NAMES
     * </pre>
     *
     * @param echo
     * @return
     */
    public List<Docker> mapperDockerContainer(String echo) {
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

    /**
     * @see Docker
     * <pre>
     *     REPOSITORY TAG IMAGE ID CREATED  SIZE
     * </pre>
     */
    public List<Docker> mapperDockerImages(String echo) {
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
}