package io.jopen.springboot.plugin.common;

import org.checkerframework.checker.nullness.qual.NonNull;

public class JopenException extends RuntimeException {
    public JopenException(@NonNull String errMsg) {
        super(errMsg);
    }
}
