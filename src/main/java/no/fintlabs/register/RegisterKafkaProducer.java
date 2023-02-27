package no.fintlabs.register;

import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.EventProducerKafka;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

@Service
public class RegisterKafkaProducer extends EventProducerKafka<AdapterContract> {
    public RegisterKafkaProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, AdapterContract.class, "adapter-register");
    }
}
