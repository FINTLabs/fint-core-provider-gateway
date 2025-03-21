package no.fintlabs.provider.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class ProviderErrorPublisher {

    private final EventProducer<ProviderError> eventProducer;
    private final EventTopicNameParameters eventName;

    public ProviderErrorPublisher(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        this.eventProducer = eventProducerFactory.createProducer(ProviderError.class);
        this.eventName = createEventName();
        eventTopicService.ensureTopic(eventName, Duration.ofDays(7).toMillis());
    }

    public void publish(ProviderError providerError) {
        log.info("Publishing provider-error to Kafka!");
        eventProducer.send(
                EventProducerRecord.<ProviderError>builder()
                        .key(UUID.randomUUID().toString())
                        .topicNameParameters(eventName)
                        .value(providerError)
                        .build()
        );
    }

    private EventTopicNameParameters createEventName() {
        return EventTopicNameParameters.builder()
                .orgId("fintlabs-no")
                .domainContext("fint-core")
                .eventName("provider-error")
                .build();
    }
}
