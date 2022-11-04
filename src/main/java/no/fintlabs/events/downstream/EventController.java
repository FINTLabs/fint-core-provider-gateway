package no.fintlabs.events.downstream;

import lombok.RequiredArgsConstructor;
import no.fintlabs.adapter.models.RequestFintEventCastable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController()
public class EventController {

    private final EventService eventService;

    @GetMapping("/event/{domainName}/{packageName}/{resourceName}")
    public List<RequestFintEventCastable> getEvents(
            @PathVariable(required = false) String domainName,
            @PathVariable(required = false) String packageName,
            @PathVariable(required = false) String resourceName
    ) {
        return eventService.getEvents(domainName, packageName, resourceName);
    }
}
