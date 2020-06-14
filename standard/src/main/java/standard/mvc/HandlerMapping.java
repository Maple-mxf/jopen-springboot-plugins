package standard.mvc;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * @author maxuefeng
 * @since 2020/5/26
 */
@FunctionalInterface
public interface HandlerMapping {

    ImmutableMap<String, HandlerMapping> handlerMapping();
}
