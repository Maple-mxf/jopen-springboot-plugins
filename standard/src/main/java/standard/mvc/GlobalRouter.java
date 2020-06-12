package standard.mvc;

import com.google.common.collect.ImmutableMap;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static org.springframework.web.servlet.HandlerMapping.LOOKUP_PATH;

/**
 * @author maxuefeng
 * @since 2020/5/26
 */
public interface GlobalRouter {

    /**
     * @see UrlPathHelper
     */
    UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

    @RequestMapping(value = "/**")
    default Object route(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @RequestBody(required = false) Map<String, Object> param) throws Throwable {

        return this.routeRequest(request, response, param);
    }


    default Object routeRequest(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @RequestBody(required = false) Map<String, Object> param) throws Throwable {

        String mappingKey = this.mappingKey(request, param);

        if (!this.handlerMapping().containsKey(mappingKey)) {
            return this.ifNotMappingReturnValue();
        }

        return this.handlerMapping().get(mappingKey).handler(request, response, param);
    }


    default Object ifNotMappingReturnValue() {
        return null;
    }


    default String mappingKey(HttpServletRequest request, @Nullable Map<String, Object> param) {
        return URL_PATH_HELPER.getLookupPathForRequest(request, LOOKUP_PATH);
    }

    /**
     * @return handlerMapping
     */
    ImmutableMap<String, Handler<Object>> handlerMapping();
}
