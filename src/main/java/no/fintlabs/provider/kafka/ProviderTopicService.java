package no.fintlabs.provider.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.common.topic.TopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTopicService {

    private final Map<String, Long> topicToRetensionMap = new HashMap<>();
    private final EntityTopicService entityTopicService;
    private final EventTopicService eventTopicService;

    public long getRetensionTime(TopicNameParameters topicNameParameters) {
        if (topicToRetensionMap.containsKey(topicNameParameters.getTopicName())) {
            return topicToRetensionMap.get(topicNameParameters.getTopicName());
        } else {
            log.error("Cant get retension time because topic is not ensured: {}", topicNameParameters.getTopicName());
            return 0L;
        }
    }

    public boolean topicHasDifferentRetentionTime(TopicNameParameters topicNameParameters, Long retentionTime) {
        return !topicToRetensionMap.getOrDefault(topicNameParameters.getTopicName(), 0L).equals(retentionTime);
    }


    public boolean topicExists(TopicNameParameters topicNameParameters) {
        return topicToRetensionMap.containsKey(topicNameParameters.getTopicName());
    }

    public void ensureTopic(EntityTopicNameParameters topicName, Long retensionTime) {
        logEnsuringOfTopic(topicName.getTopicName(), retensionTime);
        entityTopicService.ensureTopic(topicName, retensionTime);
        topicToRetensionMap.put(topicName.getTopicName(), retensionTime);
    }

    public void ensureTopic(EventTopicNameParameters topicName, Long retensionTime) {
        logEnsuringOfTopic(topicName.getTopicName(), retensionTime);
        eventTopicService.ensureTopic(topicName, retensionTime);
        topicToRetensionMap.put(topicName.getTopicName(), retensionTime);
    }

    private void logEnsuringOfTopic(String topicName, Long retensionTime) {
        log.info("Ensuring topic: {} - {}", topicName, retensionTime);
    }

}
