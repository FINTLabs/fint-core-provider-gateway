package no.fintlabs.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.event.request.RequestEventService;
import no.fintlabs.event.response.ResponseEventService;
import no.fintlabs.exception.InvalidJwtException;
import no.fintlabs.exception.InvalidOrgIdException;
import no.fintlabs.exception.NoRequestFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/event")
@RestController
public class EventController {

    private final RequestEventService requestEventService;

    private final ResponseEventService responseEventService;

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
            @RequestBody ResponseFintEvent<?> responseFintEvent) throws InvalidOrgIdException, NoRequestFoundException {
        if (corePrincipal.hasMatchingOrgId(responseFintEvent.getOrgId())) {
            log.debug("Response has been posted corr-id: {} org-id: {}", responseFintEvent.getCorrId(), responseFintEvent.getOrgId());
            responseEventService.handleEvent(responseFintEvent);
            return ResponseEntity.ok().build();
        }
        log.error("Response event orgId did not match jwt orgid. Response: {}, jwt assets: {}", responseFintEvent.getOrgId(), corePrincipal.getAssets());
        throw new InvalidOrgIdException(responseFintEvent.getOrgId());
    }

    @ExceptionHandler({NoRequestFoundException.class})
    public ResponseEntity<?> handleNoRequestFoundException(NoRequestFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({InvalidOrgIdException.class})
    public ResponseEntity<?> handleInvalidOrgIdException(InvalidOrgIdException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @ExceptionHandler({InvalidJwtException.class})
    public ResponseEntity<?> handleInvalidJwtException(InvalidJwtException exception) {
        return ResponseEntity.badRequest().build();
    }

}
