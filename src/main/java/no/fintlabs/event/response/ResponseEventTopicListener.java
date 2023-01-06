package no.fintlabs.event.response;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.event.request.RequestEventService;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Slf4j
@Service
public class ResponseEventTopicListener {
    private final EventConsumerFactoryService eventConsumerFactoryService;

    private final RequestEventService requestEventService;

    public ResponseEventTopicListener(EventConsumerFactoryService eventConsumerFactoryService, RequestEventService requestEventService) {
        this.eventConsumerFactoryService = eventConsumerFactoryService;
        this.requestEventService = requestEventService;
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
                        .build()
        ).createContainer(eventTopicNameParameters);
    }

    private void processEvent(ConsumerRecord<String, ResponseFintEvent> consumerRecord) {
        log.debug("ResponseFintEvent received: {} - {}", consumerRecord.value().getOrgId(), consumerRecord.value().getCorrId());
        requestEventService.removeEvent(consumerRecord.value().getCorrId());
    }
}
