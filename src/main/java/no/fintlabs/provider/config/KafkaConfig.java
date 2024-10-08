package no.fintlabs.provider.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Getter
@Slf4j
@Configuration
public class KafkaConfig {

    public static final int SUFFIX_LENGTH = 8;
    private final String groupIdSuffix;

    public KafkaConfig(Environment environment) {
        groupIdSuffix = RandomStringUtils.random(SUFFIX_LENGTH, true, true).toLowerCase();

        String groupId = environment.getProperty("spring.kafka.consumer.group-id");
        log.info("Group-id: %s-%s".formatted(groupId, groupIdSuffix));
    }

}
