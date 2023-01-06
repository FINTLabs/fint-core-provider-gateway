package no.fintlabs.event;

import no.fintlabs.adapter.models.RequestFintEvent;
import lombok.RequiredArgsConstructor;
import no.fintlabs.adapter.models.ResponseFintEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController()
public class EventController {

    private final RequestEventService requestEventService;

    private final ResponseEventService responseEventService;

    @GetMapping(value = {"/event/{orgId}", "/event/{orgId}/{domainName}","/event/{orgId}/{domainName}/{packageName}","/event/{orgId}/{domainName}/{packageName}/{resourceName}"})
    public ResponseEntity<List<RequestFintEvent>> getEvents(
            @PathVariable String orgId,
            @PathVariable(required = false) String domainName,
            @PathVariable(required = false) String packageName,
            @PathVariable(required = false) String resourceName,
            @RequestParam(defaultValue = "0") int size
    ) {
        if (StringUtils.isBlank(orgId)) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(
                requestEventService.getEvents(orgId, domainName, packageName, resourceName, size)
        );
    }

    @PostMapping("/event")
    public ResponseEntity<Void> postEvent(@RequestBody ResponseFintEvent responseFintEvent){
        responseEventService.handleEvent(responseFintEvent);
        return ResponseEntity.ok().build();
    }
}
