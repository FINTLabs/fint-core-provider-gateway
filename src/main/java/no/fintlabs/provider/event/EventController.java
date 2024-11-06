package no.fintlabs.provider.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.provider.event.request.RequestEventService;
import no.fintlabs.provider.event.response.ResponseEventService;
import no.fintlabs.provider.exception.InvalidOrgIdException;
import no.fintlabs.provider.exception.NoRequestFoundException;
import no.fintlabs.provider.security.AdapterRequestValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/event")
public class EventController {

    private final RequestEventService requestEventService;
    private final ResponseEventService responseEventService;
    private final AdapterRequestValidator requestValidator;

    @GetMapping(value = {"{domainName}", "{domainName}/{packageName}", "{domainName}/{packageName}/{resourceName}"})
    public ResponseEntity<List<RequestFintEvent>> getEvents(
            @AuthenticationPrincipal CorePrincipal corePrincipal,
            @PathVariable(required = false) String domainName,
            @PathVariable(required = false) String packageName,
            @PathVariable(required = false) String resourceName,
            @RequestParam(defaultValue = "0") int size
    ) {
        return ResponseEntity.ok(requestEventService.getEvents(corePrincipal.getAssets(), domainName, packageName, resourceName, size));
    }

    @PostMapping
    public ResponseEntity<Void> postEvent(
            @AuthenticationPrincipal CorePrincipal corePrincipal,
            @RequestBody ResponseFintEvent responseFintEvent
    ) throws InvalidOrgIdException, NoRequestFoundException {
        requestValidator.validateOrgId(corePrincipal, responseFintEvent.getOrgId());
//        requestValidator.validateAdapterId(corePrincipal, responseFintEvent.getAdapterId());
        // TODO: Skal vi stoppe response hvis adapteret har ikke en kontrakt? Og skal vi sjekke capabilities til kontrakten?

        responseEventService.handleEvent(responseFintEvent);
        return ResponseEntity.ok().build();
    }

}
