package no.fintlabs.kafka;

import lombok.RequiredArgsConstructor;
import no.fintlabs.kafka.common.topic.TopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final Map<String, Long> topicToRetensionMap = new HashMap<>();
    private final EntityTopicService entityTopicService;
    private final EventTopicService eventTopicService;

    public boolean topicHasDifferentRetensionTime(TopicNameParameters topicNameParameters, Long retensionTime) {
        return topicToRetensionMap.getOrDefault(topicNameParameters.getTopicName(), 0L).equals(retensionTime);
    }

    public boolean topicExists(TopicNameParameters topicNameParameters) {
        return topicToRetensionMap.containsKey(topicNameParameters.getTopicName());
    }

    public void ensureTopic(EntityTopicNameParameters topicName, Long retensionTime) {
        entityTopicService.ensureTopic(topicName, retensionTime);
        topicToRetensionMap.put(topicName.getTopicName(), retensionTime);
    }

    public void ensureTopic(EventTopicNameParameters topicName, Long retensionTime) {
        eventTopicService.ensureTopic(topicName, retensionTime);
        topicToRetensionMap.put(topicName.getTopicName(), retensionTime);
    }

}
