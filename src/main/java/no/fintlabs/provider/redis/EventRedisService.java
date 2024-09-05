package no.fintlabs.provider.redis;

import lombok.RequiredArgsConstructor;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EventRedisService {


    private final ReactiveRedisTemplate<String, RequestFintEvent> requestFintEventRedisTemplate;
    private final ReactiveRedisTemplate<String, String> responseFintEventRedisTemplate;

    private static final Duration REQUEST_FINT_EVENT_TTL = Duration.ofMinutes(80);
    private static final Duration RESPONSE_FINT_EVENT_TTL = Duration.ofDays(2);

    public Mono<Boolean> putRequestFintEvent(String corrId, RequestFintEvent event) {
        return responseExists(corrId)
                .flatMap(responseExists -> {
                    if (!responseExists) {
                        return requestFintEventRedisTemplate.opsForValue().set(corrId, event, REQUEST_FINT_EVENT_TTL);
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Mono<RequestFintEvent> getRequestFintEvent(String corrId) {
        return requestFintEventRedisTemplate.opsForValue().get(corrId);
    }

    public Mono<Boolean> putResponseFintEvent(String corrId) {
        return responseFintEventRedisTemplate.opsForValue().set(corrId, "", RESPONSE_FINT_EVENT_TTL);
    }

    public Mono<Boolean> responseExists(String corrId) {
        return responseFintEventRedisTemplate.hasKey(corrId);
    }

    public Mono<Long> removeRequestFintEvent(String corrId) {
        return requestFintEventRedisTemplate.delete(corrId);
    }

    public Flux<RequestFintEvent> getAllRequestFintEvents() {
        return requestFintEventRedisTemplate.keys("*")
                .flatMap(key -> requestFintEventRedisTemplate.opsForValue().get(key));
    }

}
