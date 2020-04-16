package io.jopen.springboot.plugin.limit;

import io.jopen.springboot.plugin.common.JopenException;

/**
 * @author maxuefeng
 * @since 2020/2/15
 */
public class LimitException extends JopenException {

    public LimitException(String errMsg) {
        super(errMsg);
    }
}
