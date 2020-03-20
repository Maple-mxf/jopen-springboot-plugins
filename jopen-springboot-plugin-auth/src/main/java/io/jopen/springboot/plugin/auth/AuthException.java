package io.jopen.springboot.plugin.auth;

import io.jopen.springboot.plugin.common.JopenException;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * @author maxuefeng
 * @since 2020/2/7
 */
public class AuthException extends JopenException {
    public AuthException(@NonNull String errMsg) {
        super(errMsg);
    }
}
