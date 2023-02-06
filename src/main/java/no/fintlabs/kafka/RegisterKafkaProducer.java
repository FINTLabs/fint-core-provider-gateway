package no.fintlabs.kafka;

import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

@Service
public class RegisterKafkaProducer extends KafkaEventProducer<AdapterContract> {

    public RegisterKafkaProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, AdapterContract.class, "adapter-register");
    }

    public void send(AdapterContract adapterContract) {
        super.send(adapterContract, adapterContract.getOrgId());
    }

}
