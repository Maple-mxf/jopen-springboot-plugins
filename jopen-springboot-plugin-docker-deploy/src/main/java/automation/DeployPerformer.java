package automation;

import com.google.common.base.Verify;
import com.jcraft.jsch.JSchException;
import com.mongodb.client.result.UpdateResult;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author maxuefeng
 * @see RemoteDeployTask
 */
@Component
@Transactional
@Slf4j
@Deprecated
public class DeployPerformer {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 主干线程池的核心数量 最大线程池数量在此基础+2
     */
    private final int coreMainProcessThreadNum = 3;

    /**
     * 使用guava进行包装线程池
     */
    private final ListeningExecutorService mainProcessExecutorDelegate;

   // @Autowired
    private ReloadGatewayConfigPerformer reloadGatewayConfigPerformer;

    /**
     * 读取SSH流的线程池
     */
    private final ExecutorService backgroundReadSSHStreamExecutor;


    public DeployPerformer() {
        BlockingQueue<Runnable> taskCacheQueue = new LinkedBlockingQueue<>(20);
        ExecutorService mainProcessExecutor = new ThreadPoolExecutor(
                coreMainProcessThreadNum,
                coreMainProcessThreadNum + 2,
                24 * 60 * 60, TimeUnit.SECONDS,
                taskCacheQueue,
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("deploy-mainProcess-%d").build(),
                (runnable, executor) -> executor.submit(runnable));
        this.mainProcessExecutorDelegate = MoreExecutors.listeningDecorator(mainProcessExecutor);
        this.backgroundReadSSHStreamExecutor = Executors.newFixedThreadPool(
                coreMainProcessThreadNum * 2,
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("deploy-background-%d").build());
    }

    @Getter
    @Setter
    public static class DockerMarker {
        private String imageName;
        private String imageTag;
    }

    /**
     *
     */
    public void run(DockerMarker marker) throws IOException, JSchException {
        Hardware hardware = hardware();
        List<Adapter> adapters = hardware.getAdapters();
        Verify.verify(adapters != null && adapters.size() > 0, String.format("服务器实例%s无账号可用", hardware.getId()));

        Adapter adapter = adapters.get(0);
        Docker docker = new Docker(marker.getImageName(), marker.getImageTag(), hardware.randomPort());

        RemoteDeployTask dockerRemoteDeployTask = new RemoteDeployTask(adapter, docker,
                backgroundReadSSHStreamExecutor, this.reloadGatewayConfigPerformer);

        ListenableFuture<DeployContainerFuture> future = this.mainProcessExecutorDelegate.submit(dockerRemoteDeployTask);

        Futures.addCallback(future, new FutureCallback<DeployContainerFuture>() {
            @Override
            public void onSuccess(@Nullable DeployContainerFuture deployContainerFuture) {
                Optional.ofNullable(deployContainerFuture)
                        .ifPresent(t -> {
                            // update cloud server info
                            updateHardware(hardware, t);

                            // 回调应用程序
                            callbackApplicationOnSuccess(t);
                        });
            }

            @Override
            public void onFailure(Throwable throwable) {
                callbackApplicationOnFailure(throwable);
            }
        }, this.mainProcessExecutorDelegate);
    }

    /**
     * @see Adapter
     * @see Hardware
     */
    private Hardware hardware() {
        Query query = new Query();
        query.addCriteria(Criteria.where("broken").is(false));
        Hardware hardware = mongoTemplate.findOne(query, Hardware.class);
        Verify.verify(hardware != null, "找不到合适的服务器实例");
        return hardware;
    }

    /**
     * 回调应用程序
     */
    private void callbackApplicationOnSuccess(DeployContainerFuture result) {
        log.info("deploy docker images success");
    }

    private void callbackApplicationOnFailure(Throwable throwable) {
        log.info("deploy docker images failure");
    }

    /**
     * 更新机器信息
     */
    private void updateHardware(Hardware hardware, DeployContainerFuture deployContainerFuture) {

        Update update = new Update();
        Set<Integer> bindedPorts = deployContainerFuture.getBindedPorts();
        if (bindedPorts != null && bindedPorts.size() > 0) update.set("usedPorts", bindedPorts);

        Hardware.DiskInfo diskInfo = deployContainerFuture.getDiskInfo();
        if (diskInfo != null
                && diskInfo.getFreeDiskCap() != null
                && diskInfo.getTotalDiskCap() != null
                && diskInfo.getUsedCap() != null)
            update.set("diskInfo", diskInfo);

        Hardware.MemoryInfo memoryInfo = deployContainerFuture.getMemoryInfo();
        if (memoryInfo != null
                && memoryInfo.getFreeMemoryCap() != null
                && memoryInfo.getTotalMemoryCap() != null
                && memoryInfo.getUsedCap() != null)
            update.set("memoryInfo", memoryInfo);

        update.set("updateTime", new Date().getTime());
        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(hardware.getId())),
                update,
                Hardware.class);

        log.info("Hardware update result {} ", updateResult);

        Docker dockerInfo = deployContainerFuture.getDocker();
        if (dockerInfo != null) {
            dockerInfo.setHardwareId(hardware.getId());
            mongoTemplate.save(dockerInfo);
        }
    }
}
