package no.fintlabs.kafka;

import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.util.concurrent.ListenableFuture;

public abstract class KafkaEventProducer<T> {

    private final EventProducer<T> adapterEventProducer;
    private final EventTopicService eventTopicService;
    private String eventName;

    public KafkaEventProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService, Class<T> valueClass, String eventName) {
        this.eventTopicService = eventTopicService;
        this.eventName = eventName;
        this.adapterEventProducer = eventProducerFactory.createProducer(valueClass);
    }

    public ListenableFuture send(T value, String orgId) {
        EventTopicNameParameters eventTopicNameParameters = generateTopicName(orgId);
        return adapterEventProducer.send(createEventProducerRecord(value, eventTopicNameParameters));
    }

    public void ensureTopic(String ordId, long retentionTimeMs) {
        // Todo See CT-457 for reference
        eventTopicService.ensureTopic(generateTopicName(ordId), retentionTimeMs);
    }

    public EventProducerRecord createEventProducerRecord(T value, EventTopicNameParameters topicName) {
        return EventProducerRecord.builder()
                .topicNameParameters(topicName)
                .value(value)
                .build();
    }


    public EventTopicNameParameters generateTopicName(String orgId) {
        return EventTopicNameParameters
                .builder()
                .orgId(orgId)
                .eventName(eventName)
                .build();
    }

}
