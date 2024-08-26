package no.fintlabs.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.*;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.provider.datasync.SyncPageService;
import no.fintlabs.provider.heartbeat.HeartbeatService;
import no.fintlabs.provider.register.RegistrationService;
import no.fintlabs.provider.security.AdapterRequestValidator;
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

    private final AdapterRequestValidator requestValidator;
    private final RegistrationService registrationService;
    private final HeartbeatService heartbeatService;
    private final SyncPageService syncPageService;

    @GetMapping("status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal CorePrincipal corePrincipal) {
        return ResponseEntity.ok(Map.of(
                "status", "Greetings form FINTLabs 👋",
                "corePrincipal", corePrincipal));
    }

    @PostMapping("heartbeat")
    public ResponseEntity<String> heartbeat(@AuthenticationPrincipal CorePrincipal corePrincipal,
                                            @RequestBody AdapterHeartbeat adapterHeartbeat) {
        requestValidator.validateOrgId(corePrincipal, adapterHeartbeat.getOrgId());
        requestValidator.validateUsername(corePrincipal, adapterHeartbeat.getUsername());

        heartbeatService.beat(adapterHeartbeat);
        return ResponseEntity.ok("💗");
    }

    @PostMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> fullSync(@AuthenticationPrincipal CorePrincipal corePrincipal,
                                         @RequestBody FullSyncPageOfObject syncPage,
                                         @PathVariable final String domain,
                                         @PathVariable final String packageName,
                                         @PathVariable final String entity) {
        requestValidator.validateOrgId(corePrincipal, syncPage.getMetadata().getOrgId());
        requestValidator.validateRole(corePrincipal, domain, packageName);
        requestValidator.validateAdapterId(corePrincipal, syncPage.getMetadata().getAdapterId());

        syncPageService.doSync(syncPage, domain, packageName, entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deltaSync(
            @AuthenticationPrincipal CorePrincipal corePrincipal,
            @RequestBody DeltaSyncPageOfObject syncPage,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {
        requestValidator.validateOrgId(corePrincipal, syncPage.getMetadata().getOrgId());
        requestValidator.validateRole(corePrincipal, domain, packageName);
        requestValidator.validateAdapterId(corePrincipal, syncPage.getMetadata().getAdapterId());

        syncPageService.doSync(syncPage, domain, packageName, entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deleteSync(
            @AuthenticationPrincipal CorePrincipal corePrincipal,
            @RequestBody DeleteSyncPageOfObject syncPage,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {
        requestValidator.validateOrgId(corePrincipal, syncPage.getMetadata().getOrgId());
        requestValidator.validateRole(corePrincipal, domain, packageName);
        requestValidator.validateAdapterId(corePrincipal, syncPage.getMetadata().getAdapterId());

        syncPageService.doSync(syncPage, domain, packageName, entity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("register")
    public ResponseEntity<Void> register(@AuthenticationPrincipal CorePrincipal corePrincipal,
                                         @RequestBody final AdapterContract adapterContract) {
        requestValidator.validateOrgId(corePrincipal, adapterContract.getOrgId());
        requestValidator.validateUsername(corePrincipal, adapterContract.getUsername());

        registrationService.register(adapterContract);
        return ResponseEntity.ok().build();
    }

}
