package no.fintlabs.provider.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.common.topic.TopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import no.fintlabs.provider.redis.TopicRedisService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTopicService {

    private final EntityTopicService entityTopicService;
    private final EventTopicService eventTopicService;
    private final TopicRedisService topicRedisService;

    public void ensureTopic(TopicNameParameters topicNameParameters, Long retentionTime) {
        String topicName = topicNameParameters.getTopicName();

        topicRedisService.exists(topicName).subscribe(topicExists -> {
            if (topicExists) {
                topicRedisService.getRetentionTime(topicName).subscribe(existingRetentionTime -> {
                    if (!retentionTime.equals(existingRetentionTime)) {
                        log.info("Topic retention is different, ensuring new topic: {} with retention: {}", topicName, retentionTime);
                        ensureAndSaveTopic(topicNameParameters, retentionTime);
                    }
                });
            } else {
                log.info("Creating new topic: {} with retention time: {}", topicName, retentionTime);
                ensureAndSaveTopic(topicNameParameters, retentionTime);
            }
        });
    }

    private void ensureAndSaveTopic(TopicNameParameters topicNameParameters, Long retentionTime) {
        if (topicNameParameters instanceof EntityTopicNameParameters) {
            entityTopicService.ensureTopic((EntityTopicNameParameters) topicNameParameters, retentionTime);
        } else if (topicNameParameters instanceof EventTopicNameParameters) {
            eventTopicService.ensureTopic((EventTopicNameParameters) topicNameParameters, retentionTime);
        }
        topicRedisService.setTopic(topicNameParameters.getTopicName(), retentionTime).subscribe();
    }

}
