package no.fintlabs.provider.redis.config;

import lombok.Data;

@Data
public class RedisProperties {
    private String host;
    private int port;
}
