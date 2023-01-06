package no.fintlabs.event.response;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.event.request.RequestEventService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ResponseEventService {

    private final ResponseEventTopicProducer responseEventTopicProducer;

    private final RequestEventService requestEventService;

    public ResponseEventService(ResponseEventTopicProducer responseEventTopicProducer, RequestEventService requestEventService) {
        this.responseEventTopicProducer = responseEventTopicProducer;
        this.requestEventService = requestEventService;
    }

    public void handleEvent(ResponseFintEvent responseFintEvent) {

        Optional<RequestFintEvent> requestEvent = requestEventService.getEvent(responseFintEvent.getCorrId());

        if (requestEvent.isEmpty()) {
            log.error("Recieved event response, but did not find request for corr-id: {}", responseFintEvent.getCorrId());
            throw new NullPointerException("No corresponding request found for response.");
        }

        if (!responseFintEvent.getOrgId().equals(requestEvent.get().getOrgId())) {
            throw new IllegalArgumentException("OrgId for response did not match the request.");
        }

        responseEventTopicProducer.sendEvent(responseFintEvent, requestEvent.get());

        // todo: send entity
    }
}
