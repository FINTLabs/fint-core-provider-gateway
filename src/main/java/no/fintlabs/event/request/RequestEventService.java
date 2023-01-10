package no.fintlabs.event.request;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.RequestFintEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class RequestEventService {

    private final List<RequestFintEvent> events = new ArrayList<>();

    public void addEvent(RequestFintEvent event) {
        events.add(event);
    }

    public void removeEvent(String corrId) {
        Optional<RequestFintEvent> request = getEvent(corrId);
        request.ifPresent(events::remove);
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

    public Optional<RequestFintEvent> getEvent(String corrId) {
        return events
                .stream()
                .filter(e -> e.getCorrId().equals(corrId))
                .findFirst();
    }
}
