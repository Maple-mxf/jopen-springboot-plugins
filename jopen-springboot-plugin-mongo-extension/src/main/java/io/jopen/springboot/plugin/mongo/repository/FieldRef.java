package io.jopen.springboot.plugin.mongo.repository;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FieldRef {

    // ConvertRef supplier();

    @FunctionalInterface
    interface ConvertRef {
        <T> T call(Object refId);
    }
}
