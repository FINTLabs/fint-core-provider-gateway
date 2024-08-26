package no.fintlabs.provider.register;

import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import no.fintlabs.provider.kafka.EventProducerKafka;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.ADAPTER_REGISTER_EVENT_NAME;

@Service
public class RegisterKafkaProducer extends EventProducerKafka<AdapterContract> {
    public RegisterKafkaProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, AdapterContract.class, ADAPTER_REGISTER_EVENT_NAME);
    }
}
