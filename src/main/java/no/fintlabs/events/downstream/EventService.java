package no.fintlabs.events.downstream;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.RequestFintEventCastable;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventService {

    private final EventConsumerFactoryService eventConsumerFactoryService;

    private final ArrayList<RequestFintEventCastable> events;

    public EventService(EventConsumerFactoryService eventConsumerFactoryService) {
        this.eventConsumerFactoryService = eventConsumerFactoryService;
        events = new ArrayList<>();
    }

    @PostConstruct
    private void init() {

        // Topic example: fintlabs-no.fint-core.event.personvern-samtykke-samtykke-create-request
        EventTopicNamePatternParameters eventTopicNameParameters = EventTopicNamePatternParameters
                .builder()
                .orgId(FormattedTopicComponentPattern.any())
                .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))  // Optional if set as application property
                .eventName(ValidatedTopicComponentPattern.endingWith("-request"))
                .build();

        eventConsumerFactoryService.createFactory(
                RequestFintEventCastable.class,
                this::processEvent
        ).createContainer(eventTopicNameParameters);
    }

    private void processEvent(ConsumerRecord<String, RequestFintEventCastable> consumerRecord) {
        log.info("You got a " + consumerRecord.value().getValue().getClass().getName());
        events.add(consumerRecord.value());
    }

    public List<RequestFintEventCastable> getEvents(String domainName, String packageName, String resourceName){
        return events
                .stream()
                .filter(events -> StringUtils.isBlank(domainName) || events.getDomainName().equalsIgnoreCase(domainName))
                .filter(events -> StringUtils.isBlank(packageName) || events.getPackageName().equalsIgnoreCase(packageName))
                .filter(events -> StringUtils.isBlank(resourceName) || events.getPackageName().equalsIgnoreCase(resourceName))
                .collect(Collectors.toList());
    }
}
