package no.fintlabs.provider.event.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import no.fintlabs.provider.config.KafkaConfig;
import no.fintlabs.provider.event.request.RequestEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINT_CORE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseFintEventConsumer {

    private final EventConsumerFactoryService eventConsumerFactoryService;
    private final RequestEventService requestEventService;
    private final KafkaConfig kafkaConfig;

    @Bean
    public ConcurrentMessageListenerContainer<String, ResponseFintEvent> registerResponseFintEventListener() {
        return eventConsumerFactoryService.createFactory(
                ResponseFintEvent.class,
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
                        .domainContext(FormattedTopicComponentPattern.anyOf(FINT_CORE))
                        .eventName(ValidatedTopicComponentPattern.endingWith("-response"))
                        .build()
        );
    }

    private void processEvent(ConsumerRecord<String, ResponseFintEvent> consumerRecord) {
        requestEventService.removeEvent(consumerRecord.value().getCorrId());
    }

}
