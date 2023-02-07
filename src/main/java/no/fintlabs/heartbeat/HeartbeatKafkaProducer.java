package no.fintlabs.heartbeat;

import no.fintlabs.adapter.models.AdapterHeartbeat;
import no.fintlabs.kafka.EventProducerKafka;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

@Service
public class HeartbeatKafkaProducer extends EventProducerKafka<AdapterHeartbeat> {

    public HeartbeatKafkaProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, AdapterHeartbeat.class, "adapter-health");
    }

    public void send(AdapterHeartbeat adapterHeartbeat) {
        super.send(adapterHeartbeat, adapterHeartbeat.getOrgId());
    }

}
