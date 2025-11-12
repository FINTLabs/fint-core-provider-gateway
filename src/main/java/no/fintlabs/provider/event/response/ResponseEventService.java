package no.fintlabs.provider.event.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.adapter.operation.OperationType;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.provider.datasync.EntityProducer;
import no.fintlabs.provider.event.request.RequestEventService;
import no.fintlabs.provider.exception.InvalidOrgIdException;
import no.fintlabs.provider.exception.InvalidResponseFintEventException;
import no.fintlabs.provider.exception.InvalidSyncPageEntryException;
import no.fintlabs.provider.exception.NoRequestFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseEventService {

    private final ResponseEventTopicProducer responseEventTopicProducer;
    private final RequestEventService requestEventService;
    private final EntityProducer entityProducer;

    public void handleEvent(ResponseFintEvent responseFintEvent, CorePrincipal corePrincipal) throws NoRequestFoundException, InvalidOrgIdException {
        RequestFintEvent requestEvent = requestEventService.getEvent(responseFintEvent.getCorrId())
                .orElseThrow(() -> new NoRequestFoundException(responseFintEvent.getCorrId()));

        validateEvent(requestEvent, responseFintEvent, corePrincipal);

        responseEventTopicProducer.sendEvent(responseFintEvent, requestEvent);

        if (!createRequestFailed(responseFintEvent) && eventIsNotValidate(responseFintEvent)) {
            entityProducer.sendEventEntity(requestEvent, responseFintEvent.getValue());
        } else {
            log.info("Not sending entity to Kafka because it is a validate event or create request failed");
        }
    }

    private void validateEvent(RequestFintEvent request, ResponseFintEvent response, CorePrincipal corePrincipal) throws InvalidOrgIdException {
        if (Objects.isNull(response.getOperationType())) {
            log.error("Recieved event with no OperationType, returning BAD_REQUEST for {}", corePrincipal.getUsername());
            throw new InvalidResponseFintEventException("OperationType is required but was not provided.");
        }

        if (!response.getOrgId().equals(request.getOrgId())) {
            log.error("Recieved event response, did not match request org-id: {} for {}", response.getOrgId(), corePrincipal.getUsername());
            throw new InvalidOrgIdException(response.getOrgId());
        }

        if (syncPageEntryIsNullWhenRequired(response)) {
            log.error("Recieved a SyncPageEntry that is null for {}", corePrincipal.getUsername());
            throw new InvalidSyncPageEntryException("SyncPageEntry is null");
        }
    }

    private boolean createRequestFailed(ResponseFintEvent responseFintEvent) {
        return responseFintEvent.getOperationType().equals(OperationType.CREATE) && (responseHasAnError(responseFintEvent));
    }

    private boolean responseHasAnError(ResponseFintEvent responseFintEvent) {
        return responseFintEvent.isFailed() || responseFintEvent.isConflicted() || responseFintEvent.isRejected();
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
