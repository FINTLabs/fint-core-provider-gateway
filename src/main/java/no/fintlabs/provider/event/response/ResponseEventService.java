package no.fintlabs.provider.event.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.provider.datasync.EntityProducerKafka;
import no.fintlabs.provider.event.request.RequestEventService;
import no.fintlabs.provider.exception.InvalidOrgIdException;
import no.fintlabs.provider.exception.NoRequestFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINT_CORE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseEventService {

    private final ResponseEventTopicProducer responseEventTopicProducer;
    private final RequestEventService requestEventService;
    private final EntityProducerKafka entityProducerKafka;

    public Mono<Object> handleEvent(ResponseFintEvent responseFintEvent) {
        return requestEventService.getEvent(responseFintEvent.getCorrId())
                .switchIfEmpty(Mono.error(new NoRequestFoundException(responseFintEvent.getCorrId())))
                .flatMap(requestFintEvent -> {
                    if (!responseFintEvent.getOrgId().equals(requestFintEvent.getOrgId())) {
                        log.error("Received event response, did not match request org-id: {}", responseFintEvent.getOrgId());
                        return Mono.error(new InvalidOrgIdException(responseFintEvent.getOrgId()));
                    }

                    responseEventTopicProducer.sendEvent(responseFintEvent, requestFintEvent);

                    entityProducerKafka.sendEntity(
                            EntityTopicNameParameters.builder()
                                    .orgId(responseFintEvent.getOrgId())
                                    .domainContext(FINT_CORE)
                                    .resource(getResourceName(requestFintEvent))
                                    .build(),
                            responseFintEvent.getValue(),
                            responseFintEvent.getCorrId()
                    );

                    return Mono.empty();
                })
                .doOnError(error -> log.error("Error handling event: ", error));
    }


    private String getResourceName(RequestFintEvent requestFintEvent) {
        return "%s-%s-%s".formatted(
                requestFintEvent.getDomainName(),
                requestFintEvent.getPackageName(),
                requestFintEvent.getResourceName()
        );
    }

}
