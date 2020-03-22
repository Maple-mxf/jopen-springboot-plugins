package io.jopen.springboot.plugin.mongo.quartz.config;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * @author maxuefeng
 * @see io.jopen.springboot.plugin.mongo.quartz.MongoDBJobStore
 * @since 2020/3/21
 */
@Configuration
@AutoConfigureBefore(value = MongoAutoConfiguration.class)
public class SpringBootQuartzAutoConfiguration {


}
