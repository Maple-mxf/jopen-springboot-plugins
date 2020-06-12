package standard.mvc;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


/**
 * @author maxuefeng
 * @since 2020/5/28
 */
@Getter
@Setter
@EqualsAndHashCode
public class MappingHandlerEndpoint<T, E, Response> {

    private String action;
    private Class<E> mapperType;
    private BiThrowingFunction<T, Class<E>, E> function;
    private FunctionHandler<E, Response> handler;

    public MappingHandlerEndpoint(String action, Class<E> mapperType, BiThrowingFunction<T, Class<E>, E> function, FunctionHandler<E, Response> handler) {
        this.action = action;
        this.mapperType = mapperType;
        this.function = function;
        this.handler = handler;
    }
}
