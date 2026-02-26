package no.fintlabs.provider.kafka;

import lombok.extern.slf4j.Slf4j;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EventTopicService;
import no.novari.kafka.topic.configuration.EventCleanupFrequency;
import no.novari.kafka.topic.configuration.EventTopicConfiguration;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import no.novari.kafka.topic.name.TopicNamePrefixParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class ProviderErrorPublisher {

    private static final Duration RETENTION_TIME = Duration.ofDays(7);
    private static final int PARTITIONS = 1;
    private final ParameterizedTemplate<ProviderError> eventProducer;
    private final EventTopicNameParameters eventName;

    public ProviderErrorPublisher(ParameterizedTemplateFactory parameterizedTemplateFactory, EventTopicService eventTopicService) {
        this.eventProducer = parameterizedTemplateFactory.createTemplate(ProviderError.class);
        this.eventName = createEventName();
        eventTopicService.createOrModifyTopic(
                eventName,
                EventTopicConfiguration
                        .stepBuilder()
                        .partitions(PARTITIONS)
                        .retentionTime(RETENTION_TIME)
                        .cleanupFrequency(EventCleanupFrequency.NORMAL)
                        .build()
        );
    }

    public void publish(ProviderError providerError) {
        log.info("Publishing provider-error to Kafka!");
        eventProducer.send(
                ParameterizedProducerRecord.<ProviderError>builder()
                        .key(UUID.randomUUID().toString())
                        .topicNameParameters(eventName)
                        .value(providerError)
                        .build()
        );
    }

    private EventTopicNameParameters createEventName() {
        return EventTopicNameParameters.builder()
                .topicNamePrefixParameters(
                        TopicNamePrefixParameters
                                .stepBuilder()
                                .orgIdApplicationDefault()
                                .domainContextApplicationDefault()
                                .build()
                )
                .eventName("provider-error")
                .build();
    }
}
