package no.fintlabs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.*;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.datasync.DataSyncService;
import no.fintlabs.heartbeat.HeartbeatService;
import no.fintlabs.register.RegisterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal CorePrincipal corePrincipal) {
        return ResponseEntity.ok(Map.of(
                "status", "Greetings form FINTLabs 👋",
                "corePrincipal", corePrincipal));
    }

    @PostMapping("heartbeat")
    public ResponseEntity<String> heartbeat(@AuthenticationPrincipal CorePrincipal corePrincipal,
                                            @RequestBody AdapterHeartbeat adapterHeartbeat) {
        heartbeatService.register(adapterHeartbeat, corePrincipal);
        return ResponseEntity.ok("💗");
    }

    @PostMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> fullSync(@AuthenticationPrincipal CorePrincipal corePrincipal,
                                         @RequestBody FullSyncPageOfObject entities,
                                         @PathVariable final String domain,
                                         @PathVariable final String packageName,
                                         @PathVariable final String entity) {

        dataSyncService.registerSync(corePrincipal, entities, domain, packageName, entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deltaSync(
            @AuthenticationPrincipal CorePrincipal corePrincipal,
            @RequestBody DeltaSyncPageOfObject entities,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {

        dataSyncService.registerSync(corePrincipal, entities, domain, packageName, entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deleteSync(
            @AuthenticationPrincipal CorePrincipal corePrincipal,
            @RequestBody DeleteSyncPageOfObject entities,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {

        dataSyncService.registerSync(corePrincipal, entities, domain, packageName, entity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("register")
    public ResponseEntity<Void> register(@AuthenticationPrincipal CorePrincipal corePrincipal,
                                         @RequestBody final AdapterContract adapterContract) {
        registerService.register(adapterContract, corePrincipal);
        return ResponseEntity.ok().build();
    }

}
