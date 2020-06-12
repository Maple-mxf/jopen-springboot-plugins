package standard.mvc;

import org.springframework.lang.NonNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author maxuefeng
 * @since 2020/5/28
 */
@FunctionalInterface
public interface FunctionHandler<T, R> {

    R handler(HttpServletRequest request,
              HttpServletResponse response,
              T t) throws Throwable;
}
