package no.fintlabs.event.response;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FintCoreKafkaAdapterService;
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

    private final FintCoreKafkaAdapterService kafkaAdapterService;

    public ResponseEventService(ResponseEventTopicProducer responseEventTopicProducer, RequestEventService requestEventService, FintCoreKafkaAdapterService kafkaAdapterService) {
        this.responseEventTopicProducer = responseEventTopicProducer;
        this.requestEventService = requestEventService;
        this.kafkaAdapterService = kafkaAdapterService;
    }

    public void handleEvent(ResponseFintEvent responseFintEvent) {

        Optional<RequestFintEvent> optionalRequestEvent = requestEventService.getEvent(responseFintEvent.getCorrId());

        if (optionalRequestEvent.isEmpty()) {
            log.error("Recieved event response, but did not find request for corr-id: {}", responseFintEvent.getCorrId());
            throw new NullPointerException("No corresponding request found for response.");
        }

        RequestFintEvent requestEvent = optionalRequestEvent.get();

        if (!responseFintEvent.getOrgId().equals(requestEvent.getOrgId())) {
            throw new IllegalArgumentException("OrgId for response did not match the request.");
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
