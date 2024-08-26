package no.fintlabs.provider.heartbeat;

import no.fintlabs.adapter.models.AdapterHeartbeat;
import no.fintlabs.provider.kafka.EventProducerKafka;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.HEARTBEAT_EVENT_NAME;

@Service
public class HeartbeatKafkaProducer extends EventProducerKafka<AdapterHeartbeat> {
    public HeartbeatKafkaProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, AdapterHeartbeat.class, HEARTBEAT_EVENT_NAME);
    }
}