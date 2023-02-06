package no.fintlabs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AdapterRequestValidator;
import no.fintlabs.kafka.RegisterKafkaProducer;
import no.fintlabs.kafka.ensure.FintCoreEntityTopicService;
import no.fintlabs.kafka.ensure.FintCoreEventTopicService;
import no.fintlabs.SyncPageService;
import no.fintlabs.adapter.models.*;
import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import no.fintlabs.kafka.HeartbeatKafkaProducer;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping
public class ProviderController {

    private final FintCoreEntityTopicService fintCoreEntityTopicService;
    private final FintCoreEventTopicService fintCoreEventTopicService;
    private final SyncPageService syncPageService;
    private final HeartbeatKafkaProducer heartbeatKafkaProducer;
    private final RegisterKafkaProducer registerKafkaProducer;

    private final AdapterRequestValidator validator;


    public ProviderController(FintCoreEntityTopicService fintCoreEntityTopicService,
                              FintCoreEventTopicService fintCoreEventTopicService,
                              SyncPageService syncPageService,
                              HeartbeatKafkaProducer heartbeatKafkaProducer,
                              RegisterKafkaProducer registerKafkaProducer,
                              AdapterRequestValidator validator) {
        this.fintCoreEntityTopicService = fintCoreEntityTopicService;
        this.fintCoreEventTopicService = fintCoreEventTopicService;
        this.syncPageService = syncPageService;
        this.heartbeatKafkaProducer = heartbeatKafkaProducer;
        this.registerKafkaProducer = registerKafkaProducer;
        this.validator = validator;
    }

    @GetMapping("status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(Map.of(
                "status", "Greetings form FINTLabs 👋",
                "principal", principal));
    }

    @PostMapping("heartbeat")
    public ResponseEntity<String> heartbeat(@AuthenticationPrincipal Jwt principal,
                                            @RequestBody AdapterHeartbeat adapterHeartbeat) {


        log.debug("Heartbeat from adapter id: {}, orgIds: {}, username: {}",
                adapterHeartbeat.getAdapterId(),
                adapterHeartbeat.getOrgId(),
                adapterHeartbeat.getUsername()
        );

        validator.validateOrgId(principal, adapterHeartbeat.getOrgId());
        validator.validateUsername(principal, adapterHeartbeat.getUsername());

        fintCoreEventTopicService.ensureAdapterHeartbeatTopic(adapterHeartbeat);
        heartbeatKafkaProducer.send(adapterHeartbeat, adapterHeartbeat.getOrgId());

        return ResponseEntity.ok("💗");

    }

    @PostMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> fullSync(@AuthenticationPrincipal Jwt principal,
                                         @RequestBody FullSyncPageOfObject entities,
                                         @PathVariable final String domain,
                                         @PathVariable final String packageName,
                                         @PathVariable final String entity) {

        logEntities("Full sync", entities.getMetadata(), entities.getResources().size());
        validator.validateOrgId(principal, entities.getMetadata().getOrgId());
        syncPageService.doFullSync(entities, domain, packageName, entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deltaSync(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody DeltaSyncPageOfObject entities,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {

        logEntities("Delta sync", entities.getMetadata(), entities.getResources().size());
        validator.validateOrgId(principal, entities.getMetadata().getOrgId());
        syncPageService.doDeltaSync(entities, domain, packageName, entity);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("register")
    public ResponseEntity<Void> register(@AuthenticationPrincipal Jwt jwt,
                                         @RequestBody final AdapterContract adapterContract) {

        log.info("Adapter registered {}", adapterContract);

        validator.validateOrgId(jwt, adapterContract.getOrgId());
        validator.validateUsername(jwt, adapterContract.getUsername());

        fintCoreEventTopicService.ensureAdapterRegisterTopic(adapterContract);

        registerKafkaProducer.send(adapterContract);
        fintCoreEntityTopicService.ensureAdapterEntityTopics(adapterContract);
        fintCoreEventTopicService.ensureAdapterFullSyncTopic(adapterContract);
        fintCoreEventTopicService.ensureAdapterDeltaSyncTopic(adapterContract);

        return ResponseEntity.ok().build();
    }

    private static void logEntities(String syncType, SyncPageMetadata metadata, int resourceSize){
        log.info("{}: {}({}), {}, total size: {}, page size: {}, page: {}, total pages: {}",
                syncType,
                metadata.getCorrId(),
                metadata.getOrgId(),
                metadata.getUriRef(),
                metadata.getTotalSize(),
                resourceSize,
                metadata.getPage(),
                metadata.getTotalPages()
        );
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
