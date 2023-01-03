package no.fintlabs.event;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class EventService {

    private final List<RequestFintEvent> events = new ArrayList<>();

    public void addEvent(RequestFintEvent event) {
        events.add(event);
    }

    public List<RequestFintEvent> getEvents(String orgId, String domainName, String packageName, String resourceName, int size) {
        Stream<RequestFintEvent> stream = events.stream()
                .filter(event -> event.getOrgId().equals(orgId))
                .filter(event -> StringUtils.isBlank(domainName) || event.getDomainName().equalsIgnoreCase(domainName))
                .filter(event -> StringUtils.isBlank(packageName) || event.getPackageName().equalsIgnoreCase(packageName))
                .filter(event -> StringUtils.isBlank(resourceName) || event.getResourceName().equalsIgnoreCase(resourceName));

        if (size > 0) stream = stream.limit(size);

        List<RequestFintEvent> list = stream.collect(Collectors.toList());
        return list;
    }
}
