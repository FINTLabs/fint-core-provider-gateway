package no.fintlabs.provider.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.novari.kafka.topic.EntityTopicService;
import no.novari.kafka.topic.EventTopicService;
import no.novari.kafka.topic.configuration.EntityCleanupFrequency;
import no.novari.kafka.topic.configuration.EntityTopicConfiguration;
import no.novari.kafka.topic.configuration.EventCleanupFrequency;
import no.novari.kafka.topic.configuration.EventTopicConfiguration;
import no.novari.kafka.topic.name.EntityTopicNameParameters;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import no.novari.kafka.topic.name.TopicNameParameters;
import no.novari.kafka.topic.name.TopicNameService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTopicService {

    private static final int PARTITIONS = 1;
    private final Map<String, Duration> topicToRetentionMap = new ConcurrentHashMap<>();
    private final EntityTopicService entityTopicService;
    private final EventTopicService eventTopicService;
    private final TopicNameService topicNameService;

    public Duration getRetentionTime(TopicNameParameters topicNameParameters) {
        String topicName = topicNameService.validateAndMapToTopicName(topicNameParameters);
        if (topicToRetentionMap.containsKey(topicName)) {
            return topicToRetentionMap.get(topicName);
        } else {
            log.error("Cant get retention time because topic is not ensured: {}", topicName);
            return Duration.ZERO;
        }
    }

    public boolean topicHasDifferentRetentionTime(TopicNameParameters topicNameParameters, Duration retentionTime) {
        String topicName = topicNameService.validateAndMapToTopicName(topicNameParameters);
        return !topicToRetentionMap.getOrDefault(topicName, Duration.ZERO).equals(retentionTime);
    }


    public boolean topicExists(TopicNameParameters topicNameParameters) {
        String topicName = topicNameService.validateAndMapToTopicName(topicNameParameters);
        return topicToRetentionMap.containsKey(topicName);
    }

    public void createOrModifyTopic(EntityTopicNameParameters topicName, Duration retentionTime) {
        String mappedTopicName = topicNameService.validateAndMapToTopicName(topicName);
        logEnsuringOfTopic(mappedTopicName, retentionTime);
        entityTopicService.createOrModifyTopic(
                topicName,
                EntityTopicConfiguration
                        .stepBuilder()
                        .partitions(PARTITIONS)
                        .lastValueRetentionTime(retentionTime)
                        .nullValueRetentionTime(retentionTime)
                        .cleanupFrequency(EntityCleanupFrequency.NORMAL)
                        .build()
        );
        topicToRetentionMap.put(mappedTopicName, retentionTime);
    }

    public void createOrModifyTopic(EventTopicNameParameters topicName, Duration retentionTime) {
        String mappedTopicName = topicNameService.validateAndMapToTopicName(topicName);
        logEnsuringOfTopic(mappedTopicName, retentionTime);
        eventTopicService.createOrModifyTopic(
                topicName,
                EventTopicConfiguration
                        .stepBuilder()
                        .partitions(PARTITIONS)
                        .retentionTime(retentionTime)
                        .cleanupFrequency(EventCleanupFrequency.NORMAL)
                        .build()
        );
        topicToRetentionMap.put(mappedTopicName, retentionTime);
    }

    private void logEnsuringOfTopic(String topicName, Duration retentionTime) {
        log.info("Ensuring topic: {} - {}", topicName, retentionTime);
    }

}
