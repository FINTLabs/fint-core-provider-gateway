package no.fintlabs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AdapterRequestValidator;
import no.fintlabs.FintCoreEntityTopicService;
import no.fintlabs.FintCoreEventTopicService;
import no.fintlabs.FintCoreKafkaAdapterService;
import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import no.fintlabs.model.AdapterContract;
import no.fintlabs.model.AdapterPing;
import no.fintlabs.model.DeltaSyncEntity;
import no.fintlabs.model.FullSyncEntity;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping()
public class ProviderController {

    private final FintCoreEntityTopicService fintCoreEntityTopicService;
    private final FintCoreEventTopicService fintCoreEventTopicService;
    private final FintCoreKafkaAdapterService fintCoreKafkaAdapterService;


    public ProviderController(FintCoreEntityTopicService fintCoreEntityTopicService,
                              FintCoreEventTopicService fintCoreEventTopicService,
                              FintCoreKafkaAdapterService fintCoreKafkaAdapterService) {
        this.fintCoreEntityTopicService = fintCoreEntityTopicService;
        this.fintCoreEventTopicService = fintCoreEventTopicService;
        this.fintCoreKafkaAdapterService = fintCoreKafkaAdapterService;
    }

    @PostMapping("ping")
    public ResponseEntity<String> ping(@AuthenticationPrincipal Jwt principal,
                                       @RequestBody AdapterPing adapterPing) throws JsonProcessingException {


        log.info("Ping from adapter id: {}, orgIds: {}, username: {}",
                adapterPing.getAdapterId(),
                adapterPing.getOrgId(),
                adapterPing.getUsername()
        );

        AdapterRequestValidator.validateOrgId(principal, adapterPing.getOrgId());
        AdapterRequestValidator.validateUsername(principal, adapterPing.getUsername());

        fintCoreEventTopicService.ensureAdapterPingTopic(adapterPing);
        fintCoreKafkaAdapterService.ping(adapterPing);

        return ResponseEntity.ok("pong");

    }

    @PostMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> postEntities(@AuthenticationPrincipal Jwt principal,
                                             @RequestBody FullSyncEntity entities,
                                             @PathVariable final String domain,
                                             @PathVariable final String packageName,
                                             @PathVariable final String entity) throws JsonProcessingException {


        log.info("Full sync: {}, {}, {}, {}", entities.getMetadata().getOrgId(), domain, packageName, entity);

        AdapterRequestValidator.validateOrgId(principal, entities.getMetadata().getOrgId());


        for (HashMap<String, ?> resource : entities.getResources()) {
            fintCoreKafkaAdapterService.entity(entities.getMetadata().getOrgId(), domain, packageName, entity, resource);
        }

        return ResponseEntity.ok().build();
    }

    @PatchMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deltaSync(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody DeltaSyncEntity entities,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity
    ) throws JsonProcessingException {


        log.info("Delta sync: {}, {}, {}, {}", entities.getMetadata().getOrgId(), domain, packageName, entity);

        AdapterRequestValidator.validateOrgId(principal, entities.getMetadata().getOrgId());


        for (HashMap<String, ?> resource : entities.getResources()) {
            fintCoreKafkaAdapterService.entity(entities.getMetadata().getOrgId(), domain, packageName, entity, resource);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("register")
    public ResponseEntity<Void> register(@AuthenticationPrincipal Jwt jwt,
                                         @RequestBody final AdapterContract adapterContract
    ) throws JsonProcessingException {


        log.info("Adapter registered {}", adapterContract);

        AdapterRequestValidator.validateOrgId(jwt, adapterContract.getOrgId());
        AdapterRequestValidator.validateUsername(jwt, adapterContract.getUsername());

        fintCoreEventTopicService.ensureAdapterRegisterTopic(adapterContract);

        fintCoreKafkaAdapterService.register(adapterContract);
        fintCoreEntityTopicService.ensureAdapterEntityTopics(adapterContract);

        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Void> handleJsonProcessingException(Throwable e) {
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
