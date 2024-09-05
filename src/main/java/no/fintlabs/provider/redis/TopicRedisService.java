package no.fintlabs.provider.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class TopicRedisService {

    private final ReactiveRedisTemplate<String, Long> topicRedis;

    public Mono<Boolean> setTopic(String topicName, Long value) {
        return topicRedis.opsForValue().set(topicName, value);
    }

    public Mono<Long> getRetentionTime(String topicName) {
        return topicRedis.opsForValue().get(topicName);
    }

    public Mono<Boolean> exists(String topicName) {
        return topicRedis.hasKey(topicName);
    }

}
