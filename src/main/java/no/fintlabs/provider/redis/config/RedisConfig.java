package no.fintlabs.provider.redis.config;

import lombok.Data;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Data
@Configuration
@ConfigurationProperties("fint.redis")
public class RedisConfig {

    private RedisProperties provider;
    private RedisProperties topic;


    @Bean
    @Primary
    public ReactiveRedisConnectionFactory topicRedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(topic.getHost());
        redisStandaloneConfiguration.setPort(topic.getPort());

        return new LettuceConnectionFactory(redisStandaloneConfiguration, LettuceClientConfiguration.defaultConfiguration());
    }

    @Bean
    public ReactiveRedisConnectionFactory providerRedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(provider.getHost());
        redisStandaloneConfiguration.setPort(provider.getPort());

        return new LettuceConnectionFactory(redisStandaloneConfiguration, LettuceClientConfiguration.defaultConfiguration());
    }

    @Bean
    public ReactiveRedisTemplate<String, Long> topicRedis(ReactiveRedisConnectionFactory providerRedisConnectionFactory) {
        RedisSerializationContext<String, Long> context = RedisSerializationContext
                .<String, Long>newSerializationContext(new StringRedisSerializer())
                .hashKey(new StringRedisSerializer())
                .hashValue(new StringRedisSerializer())
                .value(new Jackson2JsonRedisSerializer<>(Long.class))
                .build();

        return new ReactiveRedisTemplate<>(providerRedisConnectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, RequestFintEvent> requestFintEventRedisTemplate(
            ReactiveRedisConnectionFactory providerRedisConnectionFactory) {

        RedisSerializationContext<String, RequestFintEvent> context = RedisSerializationContext
                .<String, RequestFintEvent>newSerializationContext(new StringRedisSerializer())
                .hashKey(new StringRedisSerializer())
                .hashValue(new Jackson2JsonRedisSerializer<>(RequestFintEvent.class))
                .value(new Jackson2JsonRedisSerializer<>(RequestFintEvent.class))
                .build();

        return new ReactiveRedisTemplate<>(providerRedisConnectionFactory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, String> responseFintEventRedisTemplate(
            ReactiveRedisConnectionFactory providerRedisConnectionFactory) {

        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(new StringRedisSerializer())
                .hashKey(new StringRedisSerializer())
                .hashValue(new StringRedisSerializer())
                .value(new StringRedisSerializer())
                .build();

        return new ReactiveRedisTemplate<>(providerRedisConnectionFactory, context);
    }



}
