package no.fintlabs.kafka;

import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.adapter.models.AdapterHeartbeat;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

@Service
public class HeartbeatKafkaProducer extends KafkaEventProducer<AdapterHeartbeat> {

    public HeartbeatKafkaProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, AdapterHeartbeat.class, "adapter-health");
    }

    public void send(AdapterHeartbeat adapterHeartbeat) {
        super.send(adapterHeartbeat, adapterHeartbeat.getOrgId());
    }

}
