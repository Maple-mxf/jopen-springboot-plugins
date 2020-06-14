package standard.mvc;

import org.springframework.lang.NonNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * handler应该拆分为两层
 * 1 参数检测
 * 2 正常执行
 *
 * @author maxuefeng
 * @since 2020/5/26
 */
@FunctionalInterface
public interface Handler<T> {

    T handler(HttpServletRequest request,
              HttpServletResponse response,
              @NonNull Map<String, Object> body) throws Throwable;

}
