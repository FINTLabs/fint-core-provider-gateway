package no.fintlabs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.AdapterRequestValidator;
import no.fintlabs.FintCoreEntityTopicService;
import no.fintlabs.FintCoreEventTopicService;
import no.fintlabs.FintCoreKafkaAdapterService;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.adapter.models.AdapterPing;
import no.fintlabs.adapter.models.DeltaSyncEntityOfObject;
import no.fintlabs.adapter.models.FullSyncEntityMapOfObject;
import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping
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
                                             @RequestBody FullSyncEntityMapOfObject entities,
                                             @PathVariable final String domain,
                                             @PathVariable final String packageName,
                                             @PathVariable final String entity) throws JsonProcessingException {


        log.info("Full sync: {}, {}, {}, {}", entities.getMetadata().getOrgId(), domain, packageName, entity);

        AdapterRequestValidator.validateOrgId(principal, entities.getMetadata().getOrgId());
        Instant start = Instant.now();
        entities.getResources().forEach(
                resource ->

                {
                    try {
                        fintCoreKafkaAdapterService
                                .entity(
                                        entities.getMetadata().getOrgId(),
                                        domain,
                                        packageName,
                                        entity, resource
                                ).get();
                        //log.info(stringObjectSendResult.toString());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
//                        fintCoreKafkaAdapterService
//                                .entity(
//                                        entities.getMetadata().getOrgId(),
//                                        domain,
//                                        packageName,
//                                        entity, resource
//                                )
//                                .addCallback(new ListenableFutureCallback<Object>(){
//
//                                    @Override
//                                    public void onSuccess(Object result) {
//                                        log.info("Data sent to Kafka successfully!");
//                                    }
//
//                                    @Override
//                                    public void onFailure(Throwable ex) {
//                                        log.error("An error occurred sending data to Kafka!");
//                                    }
//                                })
        );
        Instant finish = Instant.now();
        Duration timeElapsed = Duration.between(start, finish);
        log.info("End full sync. It took {} hours, {} minutes, {} seconds to complete", timeElapsed.toHoursPart(), timeElapsed.toMinutesPart(), timeElapsed.toSecondsPart());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("{domain}/{packageName}/{entity}")
    public ResponseEntity<Void> deltaSync(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody DeltaSyncEntityOfObject entities,
            @PathVariable final String domain,
            @PathVariable final String packageName,
            @PathVariable final String entity
    ) throws JsonProcessingException {


        log.info("Delta sync: {}, {}, {}, {}", entities.getMetadata().getOrgId(), domain, packageName, entity);

        AdapterRequestValidator.validateOrgId(principal, entities.getMetadata().getOrgId());


        entities.getResources().forEach(
                resource ->
                        fintCoreKafkaAdapterService
                                .entity(
                                        entities.getMetadata().getOrgId(),
                                        domain,
                                        packageName,
                                        entity,
                                        resource
                                )
        );


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
