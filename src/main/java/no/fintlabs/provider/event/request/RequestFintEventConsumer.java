package no.fintlabs.provider.event.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import no.fintlabs.metamodel.MetamodelService;
import no.fintlabs.provider.config.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestFintEventConsumer {

    private final EventConsumerFactoryService eventConsumerFactoryService;
    private final RequestEventService requestEventService;
    private final MetamodelService metamodelService;
    private final KafkaConfig kafkaConfig;

    @Bean
    public ConcurrentMessageListenerContainer<String, RequestFintEvent> registerRequestFintEventListener() {
        return eventConsumerFactoryService.createFactory(
                RequestFintEvent.class,
                this::processEvent,
                EventConsumerConfiguration
                        .builder()
                        .seekingOffsetResetOnAssignment(true)
                        .groupIdSuffix(kafkaConfig.getGroupIdSuffix())
                        .build()
        ).createContainer(
                EventTopicNamePatternParameters
                        .builder()
                        .orgId(FormattedTopicComponentPattern.any())
                        .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                        .eventName(ValidatedTopicComponentPattern.anyOf(createEventNames()))
                        .build()
        );
    }

    private String[] createEventNames() {
        return metamodelService.getResources().stream().map(resource -> resource + "-request").toArray(String[]::new);
    }

    private void processEvent(ConsumerRecord<String, RequestFintEvent> consumerRecord) {
        log.info("RequestFintEvent received: {} - {}", consumerRecord.value().getOrgId(), consumerRecord.value().getCorrId());
        requestEventService.addEvent(consumerRecord.value());
    }
}
