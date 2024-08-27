package no.fintlabs.provider.register;

import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.provider.kafka.EventProducerKafka;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.ADAPTER_REGISTER_EVENT_NAME;

@Service
public class AdapterContractProducer extends EventProducerKafka<AdapterContract> {
    public AdapterContractProducer(EventProducerFactory eventProducerFactory) {
        super(eventProducerFactory, AdapterContract.class, ADAPTER_REGISTER_EVENT_NAME);
    }
}
