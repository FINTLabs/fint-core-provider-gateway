package no.fintlabs.kafka;

import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.util.concurrent.ListenableFuture;

public abstract class EventProducerKafka<T> {

    private final EventProducer<T> eventProducer;
    private final EventTopicService eventTopicService;

    public EventProducerKafka(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService, Class<T> valueClass) {
        this.eventTopicService = eventTopicService;
        this.eventProducer = eventProducerFactory.createProducer(valueClass);
    }

    public ListenableFuture<?> send(T value, String orgId, String eventName) {
        EventTopicNameParameters eventTopicNameParameters = generateTopicName(orgId, eventName);
        EventProducerRecord<T> eventProducerRecord = createEventProducerRecord(value, eventTopicNameParameters);
        return eventProducer.send(eventProducerRecord);
    }

    public void ensureTopic(String ordId, String eventName, long retentionTimeMs) {
        // Todo See CT-457 for reference
        eventTopicService.ensureTopic(generateTopicName(ordId, eventName), retentionTimeMs);
    }

    public EventProducerRecord<T> createEventProducerRecord(T value, EventTopicNameParameters topicName) {
        return EventProducerRecord.<T>builder()
                .topicNameParameters(topicName)
                .value(value)
                .build();
    }

    public EventTopicNameParameters generateTopicName(String orgId, String eventName) {
        return EventTopicNameParameters
                .builder()
                .orgId(orgId)
                .eventName(eventName)
                .build();
    }
}
