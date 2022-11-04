package no.fintlabs.requests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.RequestFintEventCastable;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestsService {

    private final EventConsumerFactoryService eventConsumerFactoryService;

    private ArrayList<RequestFintEventCastable> requests;

    public RequestsService(EventConsumerFactoryService eventConsumerFactoryService) {
        this.eventConsumerFactoryService = eventConsumerFactoryService;
        requests = new ArrayList<>();
    }

    @PostConstruct
    private void init() {

        // Topic example: fintlabs-no.fint-core.event.personvern-samtykke-samtykke-create-request
        EventTopicNamePatternParameters eventTopicNameParameters = EventTopicNamePatternParameters
                .builder()
                .orgId(FormattedTopicComponentPattern.any())
                .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))  // Optional if set as application property
                .eventName(ValidatedTopicComponentPattern.endingWith( "-request"))
                .build();

        eventConsumerFactoryService.createFactory(
                RequestFintEventCastable.class,
                this::processEvent
        ).createContainer(eventTopicNameParameters);
    }

    private void processEvent(ConsumerRecord<String, RequestFintEventCastable> consumerRecord) {
        log.info("You got a " + consumerRecord.value().getValue().getClass().getName());
        requests.add(consumerRecord.value());
    }
}
