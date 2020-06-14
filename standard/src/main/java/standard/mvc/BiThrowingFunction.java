package standard.mvc;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author maxuefeng
 * @see java.util.function.BiFunction
 * @since 2020/5/28
 */
@FunctionalInterface
public interface BiThrowingFunction<T, U, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result
     */
    R apply(T t, U u) throws Throwable;

}
