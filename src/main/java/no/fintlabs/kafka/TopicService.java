package no.fintlabs.kafka;

import lombok.RequiredArgsConstructor;
import no.fintlabs.kafka.common.topic.TopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final Set<String> existingTopics = new HashSet<>();
    private final EntityTopicService entityTopicService;
    private final EventTopicService eventTopicService;

    public boolean topicExists(TopicNameParameters topicNameParameters) {
        return existingTopics.contains(topicNameParameters.getTopicName());
    }

    public void ensureTopic(EntityTopicNameParameters topicName, Long retensionTime) {
        entityTopicService.ensureTopic(topicName, retensionTime);
        existingTopics.add(topicName.getTopicName());
    }

    public void ensureTopic(EventTopicNameParameters topicName, Long retensionTime) {
        eventTopicService.ensureTopic(topicName, retensionTime);
        existingTopics.add(topicName.getTopicName());
    }

}
