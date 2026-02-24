package no.fintlabs.provider

import lombok.RequiredArgsConstructor
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.adapter.models.AdapterHeartbeat
import no.fintlabs.adapter.models.sync.DeleteSyncPage
import no.fintlabs.adapter.models.sync.DeltaSyncPage
import no.fintlabs.adapter.models.sync.FullSyncPage
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal
import no.fintlabs.provider.datasync.SyncPageService
import no.fintlabs.provider.heartbeat.HeartbeatService
import no.fintlabs.provider.register.RegistrationService
import no.fintlabs.provider.security.AdapterRequestValidator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Map

@RequiredArgsConstructor
@RestController
@RequestMapping
class ProviderController(
    private val requestValidator: AdapterRequestValidator,
    private val registrationService: RegistrationService,
    private val heartbeatService: HeartbeatService,
    private val syncPageService: SyncPageService,
) {
    @GetMapping("status")
    fun status(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
    ): ResponseEntity<MutableMap<String, Any>> =
        ResponseEntity.ok<MutableMap<String, Any>>(
            Map.of<String, Any>(
                "status",
                "Greetings form FINTLabs 👋",
                "corePrincipal",
                corePrincipal,
            ),
        )

    @PostMapping("heartbeat")
    fun heartbeat(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestBody adapterHeartbeat: AdapterHeartbeat,
    ): ResponseEntity<String> {
        requestValidator.validateOrgId(corePrincipal, adapterHeartbeat.orgId)
        //        requestValidator.validateAdapterId(corePrincipal, adapterHeartbeat.getAdapterId());
        heartbeatService.beat(adapterHeartbeat)
        return ResponseEntity.ok("💗")
    }

    @PostMapping("{domain}/{packageName}/{entity}")
    fun fullSync(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestBody syncPage: FullSyncPage,
        @PathVariable domain: String,
        @PathVariable packageName: String,
        @PathVariable entity: String,
    ): ResponseEntity<Void> = handleSync(corePrincipal, syncPage, domain, packageName, entity, HttpStatus.CREATED)

    @PatchMapping("{domain}/{packageName}/{entity}")
    fun deltaSync(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestBody syncPage: DeltaSyncPage,
        @PathVariable domain: String,
        @PathVariable packageName: String,
        @PathVariable entity: String,
    ): ResponseEntity<Void> = handleSync(corePrincipal, syncPage, domain, packageName, entity, HttpStatus.CREATED)

    @DeleteMapping("{domain}/{packageName}/{entity}")
    fun deleteSync(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestBody syncPage: DeleteSyncPage,
        @PathVariable domain: String,
        @PathVariable packageName: String,
        @PathVariable entity: String,
    ): ResponseEntity<Void> = handleSync(corePrincipal, syncPage, domain, packageName, entity, HttpStatus.OK)

    @PostMapping("register")
    fun register(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestBody adapterContract: AdapterContract,
    ): ResponseEntity<Void?> {
        requestValidator.validateOrgId(corePrincipal, adapterContract.orgId)
        requestValidator.validateUsername(corePrincipal, adapterContract.username)

        registrationService.register(adapterContract)
        return ResponseEntity.ok().build<Void?>()
    }

    private fun handleSync(
        corePrincipal: CorePrincipal,
        syncPage: SyncPage,
        domain: String,
        packageName: String,
        entity: String,
        status: HttpStatus,
    ): ResponseEntity<Void> {
        requestValidator.validateOrgId(corePrincipal, syncPage.metadata.orgId)
        requestValidator.validateRole(corePrincipal, domain, packageName)
        //        requestValidator.validateAdapterId(corePrincipal, syncPage.getMetadata().getAdapterId());
        requestValidator.validateAdapterCapabilityPermission(
            syncPage.metadata.adapterId,
            domain,
            packageName,
            entity,
        )

        syncPageService.doSync(syncPage, domain, packageName, entity)
        return ResponseEntity.status(status).build()
    }
}
