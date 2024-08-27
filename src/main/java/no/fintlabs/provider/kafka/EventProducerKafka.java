package no.fintlabs.provider.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class EventProducerKafka<T> {

    private final EventProducer<T> eventProducer;
    private String eventName;

    public EventProducerKafka(EventProducerFactory eventProducerFactory, Class<T> valueClass) {
        this.eventProducer = eventProducerFactory.createProducer(valueClass);
    }

    public EventProducerKafka(EventProducerFactory eventProducerFactory, Class<T> valueClass, String eventName) {
        this(eventProducerFactory, valueClass);
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

    public EventTopicNameParameters generateTopicName(String orgId, String eventName) {
        return EventTopicNameParameters
                .builder()
                .orgId(orgId)
                .eventName(eventName)
                .build();
    }
}
