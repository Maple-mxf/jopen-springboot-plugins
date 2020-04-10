package io.jopen.springboot.plugin.quartz;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.function.Supplier;

/**
 * @see JobBeanAgent
 */
public abstract class JobBeanFunctionAgent extends QuartzJobBean {

    @NonNull
    public abstract Supplier<JobKey> jobKey();

    @NonNull
    public abstract Supplier<String> desc();

    @NonNull
    public abstract Supplier<Trigger> triggers();
}
