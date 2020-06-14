import com.google.common.collect.ImmutableBiMap;
import org.omg.CORBA.ServerRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Component
public class PersonHandler {

    public Mono<Map<String, Object>> oauth(ServerRequest request) {
        return Mono.fromFuture(
                CompletableFuture.supplyAsync((Supplier<Map<String, Object>>) () -> ImmutableBiMap.of("code", "OK")));
    }
}
