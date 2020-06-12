package standard.mvc;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.springframework.web.servlet.HandlerMapping.LOOKUP_PATH;

/**
 * @author maxuefeng
 * @since 2020/5/26
 */
public interface AutoMapperRouter<Response> {

    /**
     * @see UrlPathHelper
     */
    UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();


    default Response routeRequest(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @RequestBody(required = false) Map<String, Object> param
    ) throws Throwable {

        String mappingKey = this.mappingKey(request, param);

        List<MappingHandlerEndpoint<Map<String, Object>, Object, Response>> handlerEndpoints = this.handlerMapping();

        // 获取一个功能端点
        MappingHandlerEndpoint<Map<String, Object>, Object, Response> mappingHandlerEndpoint =
                handlerEndpoints.stream().filter(t -> t.getAction().equals(mappingKey))
                        .findFirst()
                        .orElse(null);

        if (mappingHandlerEndpoint == null) return ifNotMappingReturnValue();

        FunctionHandler<Object, Response> handler = mappingHandlerEndpoint.getHandler();

        BiThrowingFunction<Map<String, Object>, Class<Object>, Object> function = mappingHandlerEndpoint.getFunction();

        Class<?> mapperType = mappingHandlerEndpoint.getMapperType();

        Object obj = function.apply(param, (Class<Object>) mapperType);

        return handler.handler(request, response, obj);
    }


    default Response ifNotMappingReturnValue() {
        return null;
    }


    default String mappingKey(HttpServletRequest request, @Nullable Map<String, Object> param) {
        return URL_PATH_HELPER.getLookupPathForRequest(request, LOOKUP_PATH);
    }
    
    <T, E> List<MappingHandlerEndpoint<T, E, Response>> handlerMapping();
}
