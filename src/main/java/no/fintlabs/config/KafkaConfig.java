package no.fintlabs.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
public class KafkaConfig {

    public static final int SUFFIX_LENGTH = 8;
    private String groupIdSuffix;

    public KafkaConfig(Environment environment) {
        groupIdSuffix = RandomStringUtils.random(SUFFIX_LENGTH, true, true).toLowerCase();

        String groupId = environment.getProperty("spring.kafka.consumer.group-id");
        log.info(String.format("Group-id: %s-%s", groupId, groupIdSuffix));
    }

    public String getGroupIdSuffix() {
        return groupIdSuffix;
    }
}
