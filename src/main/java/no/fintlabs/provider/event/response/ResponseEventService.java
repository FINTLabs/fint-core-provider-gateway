package no.fintlabs.provider.event.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.adapter.operation.OperationType;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.provider.datasync.EntityProducerKafka;
import no.fintlabs.provider.event.request.RequestEventService;
import no.fintlabs.provider.exception.InvalidOrgIdException;
import no.fintlabs.provider.exception.InvalidResponseFintEventException;
import no.fintlabs.provider.exception.InvalidSyncPageEntryException;
import no.fintlabs.provider.exception.NoRequestFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINT_CORE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseEventService {

    private final ResponseEventTopicProducer responseEventTopicProducer;
    private final RequestEventService requestEventService;
    private final EntityProducerKafka entityProducerKafka;

    public void handleEvent(ResponseFintEvent responseFintEvent, CorePrincipal corePrincipal) throws NoRequestFoundException, InvalidOrgIdException {
        RequestFintEvent requestEvent = requestEventService.getEvent(responseFintEvent.getCorrId())
                .orElseThrow(() -> new NoRequestFoundException(responseFintEvent.getCorrId()));

        if (Objects.isNull(responseFintEvent.getOperationType())) {
            log.error("Recieved event with no OperationType, returning BAD_REQUEST for {}", corePrincipal.getUsername());
            throw new InvalidResponseFintEventException("OperationType is required but was not provided.");
        }

        if (!responseFintEvent.getOrgId().equals(requestEvent.getOrgId())) {
            log.error("Recieved event response, did not match request org-id: {} for {}", responseFintEvent.getOrgId(), corePrincipal.getUsername());
            throw new InvalidOrgIdException(responseFintEvent.getOrgId());
        }

        if (syncPageEntryIsNullWhenRequired(responseFintEvent)) {
            log.error("Recieved a SyncPageEntry that is null for {}", corePrincipal.getUsername());
            throw new InvalidSyncPageEntryException("SyncPageEntry is null");
        }

        responseEventTopicProducer.sendEvent(responseFintEvent, requestEvent);

        if (eventIsNotValidate(responseFintEvent)) {
            entityProducerKafka.sendEntity(
                    EntityTopicNameParameters.builder()
                            .orgId(responseFintEvent.getOrgId().replace(".", "-"))
                            .domainContext(FINT_CORE)
                            .resource("%s-%s-%s".formatted(requestEvent.getDomainName(), requestEvent.getPackageName(), requestEvent.getResourceName()))
                            .build(),
                    responseFintEvent.getValue(),
                    responseFintEvent.getCorrId()
            );
        }
    }

    private boolean syncPageEntryIsNullWhenRequired(ResponseFintEvent responseFintEvent) {
        if (Objects.nonNull(responseFintEvent.getOperationType()) && responseFintEvent.getOperationType().equals(OperationType.VALIDATE)) {
            return responseFintEvent.isConflicted() && responseFintEvent.getValue() == null;
        } else {
            return responseFintEvent.getValue() == null;
        }
    }


    private boolean eventIsNotValidate(ResponseFintEvent responseFintEvent) {
        return !responseFintEvent.getOperationType().equals(OperationType.VALIDATE);
    }

}
