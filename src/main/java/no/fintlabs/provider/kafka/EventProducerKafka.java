package no.fintlabs.provider.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import no.fintlabs.provider.exception.InvalidEventNameException;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class EventProducerKafka<T> {

    private final EventProducer<T> eventProducer;
    private final EventTopicService eventTopicService;
    private String eventName;

    public EventProducerKafka(EventProducerFactory eventProducerFactory,
                              EventTopicService eventTopicService,
                              Class<T> valueClass) {
        this.eventTopicService = eventTopicService;
        this.eventProducer = eventProducerFactory.createProducer(valueClass);
    }

    public EventProducerKafka(EventProducerFactory eventProducerFactory,
                              EventTopicService eventTopicService,
                              Class<T> valueClass,
                              String eventName) {
        this(eventProducerFactory, eventTopicService, valueClass);
        this.eventName = eventName;
    }

    public CompletableFuture<?> send(T value, String orgId, String eventName) {
        return eventProducer.send(
                EventProducerRecord.<T>builder()
                        .topicNameParameters(generateTopicName(orgId, eventName))
                        .value(value)
                        .build()
        );
    }

    public CompletableFuture<?> send(T value, String orgId) {
        return send(value, orgId, eventName);
    }

    public void ensureTopic(String ordId, String eventName, long retentionTimeMs) {
        // Todo See CT-457 for reference
        validateEventName(eventName);
        eventTopicService.ensureTopic(generateTopicName(ordId, eventName), retentionTimeMs);
    }

    public void ensureTopic(String ordId, long retentionTimeMs) {
        ensureTopic(ordId, eventName, retentionTimeMs);
    }

    public EventTopicNameParameters generateTopicName(String orgId, String eventName) {
        return EventTopicNameParameters
                .builder()
                .orgId(orgId)
                .eventName(eventName)
                .build();
    }

    private void validateEventName(String eventName) {
        if (eventName == null) {
            log.error("eventName is not set");
            throw new InvalidEventNameException("eventName is not set");
        }
    }
}
