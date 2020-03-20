package io.jopen.springboot.plugin.idempotency;

import io.jopen.springboot.plugin.common.JopenException;
import org.checkerframework.checker.nullness.qual.NonNull;

public class RepeatRequestException extends JopenException {

    public RepeatRequestException(@NonNull String errMsg) {
        super(errMsg);
    }
}
