package no.fintlabs.events;

import no.fintlabs.adapter.models.RequestFintEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController()
public class EventController {

    private final EventService eventService;

    @GetMapping(value = {"/event", "/event/{domainName}","/event/{domainName}/{packageName}","/event/{domainName}/{packageName}/{resourceName}"})
    public List<RequestFintEvent> getEvents(
            @PathVariable(required = false) String domainName,
            @PathVariable(required = false) String packageName,
            @PathVariable(required = false) String resourceName
    ) {
        return eventService.getEvents(domainName, packageName, resourceName);
    }
}
