package no.fintlabs.event.response;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.SyncPageService;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.event.request.RequestEventService;
import no.fintlabs.exception.InvalidOrgIdException;
import no.fintlabs.exception.NoRequestFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ResponseEventService {

    private final ResponseEventTopicProducer responseEventTopicProducer;

    private final RequestEventService requestEventService;

    private final SyncPageService kafkaAdapterService;

    public ResponseEventService(ResponseEventTopicProducer responseEventTopicProducer, RequestEventService requestEventService, SyncPageService kafkaAdapterService) {
        this.responseEventTopicProducer = responseEventTopicProducer;
        this.requestEventService = requestEventService;
        this.kafkaAdapterService = kafkaAdapterService;
    }

    public void handleEvent(ResponseFintEvent responseFintEvent) throws NoRequestFoundException, InvalidOrgIdException {

        Optional<RequestFintEvent> optionalRequestEvent = requestEventService.getEvent(responseFintEvent.getCorrId());

        if (optionalRequestEvent.isEmpty()) {
            log.error("Recieved event response, but did not find request for corr-id: {}", responseFintEvent.getCorrId());
            throw new NoRequestFoundException(responseFintEvent.getCorrId());
        }

        RequestFintEvent requestEvent = optionalRequestEvent.get();

        if (!responseFintEvent.getOrgId().equals(requestEvent.getOrgId())) {
            log.error("Recieved event response, did not match org-id: {}", responseFintEvent.getOrgId());
            throw new InvalidOrgIdException(responseFintEvent.getOrgId());
        }

        responseEventTopicProducer.sendEvent(responseFintEvent, requestEvent);

        kafkaAdapterService.sendEntity(
                responseFintEvent.getOrgId(),
                requestEvent.getDomainName(),
                requestEvent.getPackageName(),
                requestEvent.getResourceName(),
                responseFintEvent.getValue()
        );
    }
}
