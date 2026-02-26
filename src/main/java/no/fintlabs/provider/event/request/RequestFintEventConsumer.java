package no.fintlabs.provider.event.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.provider.config.KafkaConfig;
import no.fintlabs.provider.security.resource.ResourceContext;
import no.novari.kafka.consuming.ErrorHandlerConfiguration;
import no.novari.kafka.consuming.ErrorHandlerFactory;
import no.novari.kafka.consuming.ListenerConfiguration;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import no.novari.kafka.topic.name.EventTopicNamePatternParameters;
import no.novari.kafka.topic.name.TopicNamePatternParameterPattern;
import no.novari.kafka.topic.name.TopicNamePatternPrefixParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestFintEventConsumer {

    private final ParameterizedListenerContainerFactoryService parameterizedListenerContainerFactoryService;
    private final ErrorHandlerFactory errorHandlerFactory;
    private final RequestEventService requestEventService;
    private final ResourceContext resourceContext;
    private final KafkaConfig kafkaConfig;

    @Bean
    public ConcurrentMessageListenerContainer<String, RequestFintEvent> registerRequestFintEventListener() {
        return parameterizedListenerContainerFactoryService.createRecordListenerContainerFactory(
                RequestFintEvent.class,
                this::processEvent,
                ListenerConfiguration
                        .stepBuilder()
                        .groupIdApplicationDefaultWithSuffix(kafkaConfig.getGroupIdSuffix())
                        .maxPollRecordsKafkaDefault()
                        .maxPollIntervalKafkaDefault()
                        .seekToBeginningOnAssignment()
                        .build(),
                errorHandlerFactory.createErrorHandler(
                        ErrorHandlerConfiguration
                                .<RequestFintEvent>stepBuilder()
                                .noRetries()
                                .skipFailedRecords()
                                .build()
                )
        ).createContainer(
                EventTopicNamePatternParameters
                        .builder()
                        .topicNamePatternPrefixParameters(
                                TopicNamePatternPrefixParameters
                                        .stepBuilder()
                                        .orgId(TopicNamePatternParameterPattern.any())
                                        .domainContextApplicationDefault()
                                        .build()
                        )
                        .eventName(TopicNamePatternParameterPattern.anyOf(createEventNames()))
                        .build()
        );
    }

    private String[] createEventNames() {
        return resourceContext.getValidResources().stream().map(resource -> resource + "-request").toArray(String[]::new);
    }

    private void processEvent(ConsumerRecord<String, RequestFintEvent> consumerRecord) {
        log.info("RequestFintEvent received: {} - {}", consumerRecord.value().getOrgId(), consumerRecord.value().getCorrId());
        requestEventService.addEvent(consumerRecord.value());
    }
}
