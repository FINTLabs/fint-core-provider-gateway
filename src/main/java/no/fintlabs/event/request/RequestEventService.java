package no.fintlabs.event.request;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.RequestFintEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class RequestEventService {

    private static final int DAYS_TO_KEEP_REMOVED_EVENTS = 2;
    private static final int EXTRA_MINUTES_TO_KEEP_EVENTS = 20;
    private static final int DEFAULT_MINUTES_TO_KEEP_EVENTS = 60;

    private final Map<String, RequestFintEvent> events = new LinkedHashMap<>();
    private final Map<String, LocalDate> removedEvents = new HashMap<>();

    public void addEvent(RequestFintEvent event) {
        ensureEventsTimeToLive(event);

        if (removedEvents.containsKey(event.getCorrId())) {
            log.debug("Event with corrId: {} not added because in removed events", event.getCorrId());
        } else {
            events.put(event.getCorrId(), event);
            log.debug("Event with corrId: {} added", event.getCorrId());
        }
    }

    public void removeEvent(String corrId) {
        removedEvents.put(corrId, LocalDate.now());

        if (events.containsKey(corrId)) {
            events.remove(corrId);
            log.debug("Event with corrId: {} removed", corrId);
        } else {
            log.warn("Failed to remove event with corrId: {}", corrId);
        }
    }

    public List<RequestFintEvent> getEvents(String orgId, String domainName, String packageName, String resourceName, int size) {
        Stream<RequestFintEvent> stream = events.values().stream()
                .filter(event -> event.getOrgId().equals(orgId))
                .filter(event -> StringUtils.isBlank(domainName) || event.getDomainName().equalsIgnoreCase(domainName))
                .filter(event -> StringUtils.isBlank(packageName) || event.getPackageName().equalsIgnoreCase(packageName))
                .filter(event -> StringUtils.isBlank(resourceName) || event.getResourceName().equalsIgnoreCase(resourceName));

        if (size > 0) stream = stream.limit(size);

        return stream.collect(Collectors.toList());
    }

    public Optional<RequestFintEvent> getEvent(String corrId) {
        return Optional.ofNullable(events.get(corrId));
    }

    private void ensureEventsTimeToLive(RequestFintEvent event) {
        if (event.getTimeToLive() == 0) {
            long timeToLive = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(DEFAULT_MINUTES_TO_KEEP_EVENTS);
            log.debug("Setting timeToLive for event with corrId: {} to: {}", event.getCorrId(), timeToLive);
            event.setTimeToLive(timeToLive);
        }
    }

    @Scheduled(cron = "0 0 10,15 * * ?")
    protected void purgeExpiredRemovedEvents() {
        LocalDate now = LocalDate.now();
        removedEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minusDays(DAYS_TO_KEEP_REMOVED_EVENTS)));
    }

    @Scheduled(cron = "0 */15 * * * *")
    protected void purgeExpiredEvents() {
        long now = System.currentTimeMillis();
        long buffer = TimeUnit.MINUTES.toMillis(EXTRA_MINUTES_TO_KEEP_EVENTS);
        events.entrySet().removeIf(entry -> entry.getValue().getTimeToLive() + buffer < now);
    }
}
