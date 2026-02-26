package no.fintlabs.provider.event.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.provider.config.KafkaConfig;
import no.fintlabs.provider.event.request.RequestEventService;
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
public class ResponseFintEventConsumer {

    private final ParameterizedListenerContainerFactoryService parameterizedListenerContainerFactoryService;
    private final ErrorHandlerFactory errorHandlerFactory;
    private final RequestEventService requestEventService;
    private final ResourceContext resourceContext;
    private final KafkaConfig kafkaConfig;

    @Bean
    public ConcurrentMessageListenerContainer<String, ResponseFintEvent> registerResponseFintEventListener() {
        return parameterizedListenerContainerFactoryService.createRecordListenerContainerFactory(
                ResponseFintEvent.class,
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
                                .<ResponseFintEvent>stepBuilder()
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
        return resourceContext.getValidResources().stream().map(resource -> resource + "-response").toArray(String[]::new);
    }

    private void processEvent(ConsumerRecord<String, ResponseFintEvent> consumerRecord) {
        log.info("ResponseFintEvent received: {} - {}", consumerRecord.value().getOrgId(), consumerRecord.value().getCorrId());
        requestEventService.removeEvent(consumerRecord.value().getCorrId());
    }

}
