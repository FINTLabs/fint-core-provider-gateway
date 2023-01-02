package no.fintlabs.event;

import no.fintlabs.adapter.models.RequestFintEvent;
import lombok.RequiredArgsConstructor;
import no.fintlabs.adapter.models.ResponseFintEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController()
public class EventController {

    private final EventService eventService;

    @GetMapping(value = {"/event", "/event/{domainName}","/event/{domainName}/{packageName}","/event/{domainName}/{packageName}/{resourceName}"})
    public List<RequestFintEvent> getEvents(
            @PathVariable(required = false) String domainName,
            @PathVariable(required = false) String packageName,
            @PathVariable(required = false) String resourceName,
            @RequestParam(defaultValue = "0") int size
    ) {
        return eventService.getEvents(domainName, packageName, resourceName, size);
    }

    @PostMapping("/event")
    public ResponseEntity<Void> postEvent(@RequestBody ResponseFintEvent responseFintEvent){
        // update events
        // send reponse to consumer
        // send entity
        return ResponseEntity.ok().build();
    }
}
