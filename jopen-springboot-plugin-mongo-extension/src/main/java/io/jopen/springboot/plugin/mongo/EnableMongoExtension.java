package io.jopen.springboot.plugin.mongo;

import java.lang.annotation.*;

/**
 * @author maxuefeng
 * @since 2020/3/13
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EnableMongoExtension {
}
