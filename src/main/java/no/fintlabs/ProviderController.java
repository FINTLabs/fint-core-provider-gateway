package no.fintlabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.*;
import no.fintlabs.datasync.DataSyncService;
import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import no.fintlabs.heartbeat.HeartbeatService;
import no.fintlabs.register.RegisterService;
import no.fintlabs.utils.ErrorResponseMessage;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping
public class ProviderController {

    private final RegisterService registerService;
    private final HeartbeatService heartbeatService;
    private final DataSyncService dataSyncService;

    @GetMapping("status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(Map.of(
                "status", "Greetings form FINTLabs 👋",
                "principal", principal));
    }

    @PostMapping("heartbeat")
    public ResponseEntity<String> heartbeat(@AuthenticationPrincipal Jwt principal,
                                            @RequestBody AdapterHeartbeat adapterHeartbeat) {
        heartbeatService.register(adapterHeartbeat, principal);
        return ResponseEntity.ok("💗");
    }

    @PostMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> fullSync(@AuthenticationPrincipal Jwt principal,
                                         @RequestBody FullSyncPageOfObject entities,
                                         @PathVariable final String domain,
                                         @PathVariable final String packageName,
                                         @PathVariable final String entity) {

        dataSyncService.registerSync(principal, entities, domain, packageName, entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deltaSync(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody DeltaSyncPageOfObject entities,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {

        dataSyncService.registerSync(principal, entities, domain, packageName, entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deleteSync(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody DeleteSyncPageOfObject entities,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {

        dataSyncService.registerSync(principal, entities, domain, packageName, entity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("register")
    public ResponseEntity<Void> register(@AuthenticationPrincipal Jwt principal,
                                         @RequestBody final AdapterContract adapterContract) {
        registerService.register(adapterContract, principal);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Void> handleJsonProcessingException(Throwable e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(InvalidOrgId.class)
    public ResponseEntity<ErrorResponseMessage> handleInvalidOrgId(InvalidOrgId e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseMessage(e.getMessage()));
    }

    @ExceptionHandler(InvalidUsername.class)
    public ResponseEntity<ErrorResponseMessage> handleInvalidUsername(InvalidUsername e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseMessage(e.getMessage()));
    }

    @ExceptionHandler(UnknownTopicOrPartitionException.class)
    public ResponseEntity<ErrorResponseMessage> handleUnknownTopicOrPartitionException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseMessage("The adapter has probably not called the '/register' endpoint. " +
                        "Also you need to check if the entity endpoint is in the capability list."));
    }
}
