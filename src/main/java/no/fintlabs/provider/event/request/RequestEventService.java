package no.fintlabs.provider.event.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.provider.redis.EventRedisService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestEventService {

    private final EventRedisService eventRedisService;

    public void addEvent(RequestFintEvent event) {
        eventRedisService.putRequestFintEvent(event.getCorrId(), event).subscribe(requestAdded -> {
            if (requestAdded) {
                log.info("New RequestFintEvent added: {}", event.getCorrId());
            }
        });
    }

    public void removeEvent(String corrId) {
        eventRedisService.putResponseFintEvent(corrId).subscribe(responseAdded -> {
            if (responseAdded) {
                log.info("New ResponseFintEvent added: {}", corrId);
                eventRedisService.removeRequestFintEvent(corrId).subscribe(deletedAmount -> {
                    if (deletedAmount == 1L) {
                        log.info("Removed RequestFintEvent: {}", corrId);
                    }
                });
            }
        });
    }

    public Flux<RequestFintEvent> getEvents(Set<String> assets, String domainName, String packageName, String resourceName, int size) {
        return eventRedisService.getAllRequestFintEvents()
                .filter(event -> assets.contains(event.getOrgId()))
                .filter(event -> StringUtils.isBlank(domainName) || event.getDomainName().equalsIgnoreCase(domainName))
                .filter(event -> StringUtils.isBlank(packageName) || event.getPackageName().equalsIgnoreCase(packageName))
                .filter(event -> StringUtils.isBlank(resourceName) || event.getResourceName().equalsIgnoreCase(resourceName))
                .take(size > 0 ? size : Long.MAX_VALUE);
    }

    public Mono<RequestFintEvent> getEvent(String corrId) {
        return eventRedisService.getRequestFintEvent(corrId);
    }

}
