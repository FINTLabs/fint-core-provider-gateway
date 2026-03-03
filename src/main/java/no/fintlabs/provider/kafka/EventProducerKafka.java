package no.fintlabs.provider.kafka;

import lombok.extern.slf4j.Slf4j;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import no.novari.kafka.topic.name.TopicNamePrefixParameters;

@Slf4j
public abstract class EventProducerKafka<T> {

    private final ParameterizedTemplate<T> eventProducer;
    private String eventName;

    public EventProducerKafka(ParameterizedTemplateFactory parameterizedTemplateFactory, Class<T> valueClass) {
        this.eventProducer = parameterizedTemplateFactory.createTemplate(valueClass);
    }

    public EventProducerKafka(ParameterizedTemplateFactory parameterizedTemplateFactory, Class<T> valueClass, String eventName) {
        this(parameterizedTemplateFactory, valueClass);
        this.eventName = eventName;
    }

    public void send(T value, String eventName) {
        eventProducer.send(
                ParameterizedProducerRecord.<T>builder()
                        .topicNameParameters(generateTopicName(eventName))
                        .value(value)
                        .build()
        );
    }

    public void send(T value) {
        send(value, eventName);
    }

    public EventTopicNameParameters generateTopicName(String eventName) {
        return EventTopicNameParameters
                .builder()
                .topicNamePrefixParameters(
                        TopicNamePrefixParameters
                                .stepBuilder()
                                .orgIdApplicationDefault()
                                .domainContextApplicationDefault()
                                .build()
                )
                .eventName(eventName)
                .build();
    }
}
