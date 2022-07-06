package no.fintlabs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AdapterRequestValidator;
import no.fintlabs.FintCoreEntityTopicService;
import no.fintlabs.FintCoreEventTopicService;
import no.fintlabs.FintCoreKafkaAdapterService;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.adapter.models.AdapterHeartbeat;
import no.fintlabs.adapter.models.DeltaSyncPageOfObject;
import no.fintlabs.adapter.models.FullSyncPageOfObject;
import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping
public class ProviderController {

    private final FintCoreEntityTopicService fintCoreEntityTopicService;
    private final FintCoreEventTopicService fintCoreEventTopicService;
    private final FintCoreKafkaAdapterService fintCoreKafkaAdapterService;

    private final AdapterRequestValidator validator;


    public ProviderController(FintCoreEntityTopicService fintCoreEntityTopicService,
                              FintCoreEventTopicService fintCoreEventTopicService,
                              FintCoreKafkaAdapterService fintCoreKafkaAdapterService, AdapterRequestValidator validator) {
        this.fintCoreEntityTopicService = fintCoreEntityTopicService;
        this.fintCoreEventTopicService = fintCoreEventTopicService;
        this.fintCoreKafkaAdapterService = fintCoreKafkaAdapterService;
        this.validator = validator;
    }

    @PostMapping("heartbeat")
    public ResponseEntity<String> heartbeat(@AuthenticationPrincipal Jwt principal,
                                            @RequestBody AdapterHeartbeat adapterHeartbeat) {


        log.info("Heartbeat from adapter id: {}, orgIds: {}, username: {}",
                adapterHeartbeat.getAdapterId(),
                adapterHeartbeat.getOrgId(),
                adapterHeartbeat.getUsername()
        );

        validator.validateOrgId(principal, adapterHeartbeat.getOrgId());
        validator.validateUsername(principal, adapterHeartbeat.getUsername());

        fintCoreEventTopicService.ensureAdapterHeartbeatTopic(adapterHeartbeat);
        fintCoreKafkaAdapterService.heartbeat(adapterHeartbeat);

        return ResponseEntity.ok("💗");

    }

    @PostMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> fullSync(@AuthenticationPrincipal Jwt principal,
                                         @RequestBody FullSyncPageOfObject entities,
                                         @PathVariable final String domain,
                                         @PathVariable final String packageName,
                                         @PathVariable final String entity) {


        log.info("Full sync: {}({}), {}, total size: {}, page size: {}, page: {}, total pages: {}",
                entities.getMetadata().getCorrId(),
                entities.getMetadata().getOrgId(),
                entities.getMetadata().getUriRef(),
                entities.getMetadata().getTotalSize(),
                entities.getResources().size(),
                entities.getMetadata().getPage(),
                entities.getMetadata().getTotalPages()
        );

        validator.validateOrgId(principal, entities.getMetadata().getOrgId());


        //fintCoreKafkaAdapterService.sendFullSyncStatus(entities.getMetadata());
        fintCoreKafkaAdapterService.doFullSync(entities, domain, packageName, entity);


        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PatchMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deltaSync(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody DeltaSyncPageOfObject entities,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity) {


        log.info("Delta sync: {}({}), {}, total size: {}, page size: {}, page: {}, total pages: {}",
                entities.getMetadata().getCorrId(),
                entities.getMetadata().getOrgId(),
                entities.getMetadata().getUriRef(),
                entities.getMetadata().getTotalSize(),
                entities.getResources().size(),
                entities.getMetadata().getPage(),
                entities.getMetadata().getTotalPages()
        );
        validator.validateOrgId(principal, entities.getMetadata().getOrgId());

        //fintCoreKafkaAdapterService.sendDeltaSyncStatus(entities.getMetadata());
        fintCoreKafkaAdapterService.doDeltaSync(entities, domain, packageName, entity);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("register")
    public ResponseEntity<Void> register(@AuthenticationPrincipal Jwt jwt,
                                         @RequestBody final AdapterContract adapterContract) {


        log.info("Adapter registered {}", adapterContract);

        validator.validateOrgId(jwt, adapterContract.getOrgId());
        validator.validateUsername(jwt, adapterContract.getUsername());

        fintCoreEventTopicService.ensureAdapterRegisterTopic(adapterContract);

        fintCoreKafkaAdapterService.register(adapterContract);
        fintCoreEntityTopicService.ensureAdapterEntityTopics(adapterContract);
        fintCoreEventTopicService.ensureAdapterFullSyncTopic(adapterContract);
        fintCoreEventTopicService.ensureAdapterDeltaSyncTopic(adapterContract);

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
