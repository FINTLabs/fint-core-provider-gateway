package no.fintlabs.event.response;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.config.KafkaConfig;
import no.fintlabs.event.request.RequestEventService;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ResponseEventTopicListener {
    private final EventConsumerFactoryService eventConsumerFactoryService;

    private final RequestEventService requestEventService;

    private final KafkaConfig kafkaConfig;

    public ResponseEventTopicListener(EventConsumerFactoryService eventConsumerFactoryService, RequestEventService requestEventService, KafkaConfig kafkaConfig) {
        this.eventConsumerFactoryService = eventConsumerFactoryService;
        this.requestEventService = requestEventService;
        this.kafkaConfig = kafkaConfig;
    }

    @PostConstruct
    private void init() {

        // Topic example: fintlabs-no.fint-core.event.personvern-samtykke-samtykke-create-request
        EventTopicNamePatternParameters eventTopicNameParameters = EventTopicNamePatternParameters
                .builder()
                .orgId(FormattedTopicComponentPattern.any())
                .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                .eventName(ValidatedTopicComponentPattern.endingWith("-response"))
                .build();

        eventConsumerFactoryService.createFactory(
                ResponseFintEvent.class,
                this::processEvent,
                EventConsumerConfiguration
                        .builder()
                        .seekingOffsetResetOnAssignment(true)
                        .groupIdSuffix(kafkaConfig.getGroupIdSuffix())
                        .build()
        ).createContainer(eventTopicNameParameters);
    }

    private void processEvent(ConsumerRecord<String, ResponseFintEvent> consumerRecord) {
        log.debug("ResponseFintEvent received: {} - {}", consumerRecord.value().getOrgId(), consumerRecord.value().getCorrId());
        requestEventService.removeEvent(consumerRecord.value().getCorrId());
    }
}
