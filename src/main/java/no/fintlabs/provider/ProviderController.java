package no.fintlabs.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.adapter.models.AdapterHeartbeat;
import no.fintlabs.adapter.models.sync.DeleteSyncPage;
import no.fintlabs.adapter.models.sync.DeltaSyncPage;
import no.fintlabs.adapter.models.sync.FullSyncPage;
import no.fintlabs.adapter.models.sync.SyncPage;
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
//        requestValidator.validateAdapterId(corePrincipal, adapterHeartbeat.getAdapterId());

        heartbeatService.beat(adapterHeartbeat);
        return ResponseEntity.ok("💗");
    }

    @PostMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> fullSync(@AuthenticationPrincipal CorePrincipal corePrincipal,
                                         @RequestBody FullSyncPage syncPage,
                                         @PathVariable final String domain,
                                         @PathVariable final String packageName,
                                         @PathVariable final String entity) {
        return handleSync(corePrincipal, syncPage, domain, packageName, entity, HttpStatus.CREATED);
    }

    @PatchMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deltaSync(
            @AuthenticationPrincipal CorePrincipal corePrincipal,
            @RequestBody DeltaSyncPage syncPage,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {
        return handleSync(corePrincipal, syncPage, domain, packageName, entity, HttpStatus.CREATED);
    }

    @DeleteMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deleteSync(
            @AuthenticationPrincipal CorePrincipal corePrincipal,
            @RequestBody DeleteSyncPage syncPage,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {
        return handleSync(corePrincipal, syncPage, domain, packageName, entity, HttpStatus.OK);
    }

    @PostMapping("register")
    public ResponseEntity<Void> register(@AuthenticationPrincipal CorePrincipal corePrincipal,
                                         @RequestBody final AdapterContract adapterContract) {
        requestValidator.validateOrgId(corePrincipal, adapterContract.getOrgId());
        requestValidator.validateUsername(corePrincipal, adapterContract.getUsername());

        registrationService.register(adapterContract);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Void> handleSync(
            CorePrincipal corePrincipal,
            SyncPage syncPage,
            String domain,
            String packageName,
            String entity,
            HttpStatus status) {
        requestValidator.validateOrgId(corePrincipal, syncPage.getMetadata().getOrgId());
        requestValidator.validateRole(corePrincipal, domain, packageName);
//        requestValidator.validateAdapterId(corePrincipal, syncPage.getMetadata().getAdapterId());
        requestValidator.validateAdapterCapabilityPermission(syncPage.getMetadata().getAdapterId(), domain, packageName, entity);

        syncPageService.doSync(syncPage, domain, packageName, entity);
        return ResponseEntity.status(status).build();
    }

}
