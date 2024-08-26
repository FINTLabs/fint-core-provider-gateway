package no.fintlabs.provider.event.response;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.provider.event.request.RequestEventService;
import no.fintlabs.provider.exception.InvalidOrgIdException;
import no.fintlabs.provider.exception.NoRequestFoundException;
import no.fintlabs.provider.datasync.EntityProducerKafka;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ResponseEventService {

    private final ResponseEventTopicProducer responseEventTopicProducer;
    private final RequestEventService requestEventService;
    private final EntityProducerKafka entityProducerKafka;

    public ResponseEventService(
            ResponseEventTopicProducer responseEventTopicProducer,
            RequestEventService requestEventService,
            EntityProducerKafka entityProducerKafka) {
        this.responseEventTopicProducer = responseEventTopicProducer;
        this.requestEventService = requestEventService;
        this.entityProducerKafka = entityProducerKafka;
    }

    public void handleEvent(ResponseFintEvent responseFintEvent) throws NoRequestFoundException, InvalidOrgIdException {

        RequestFintEvent requestEvent = requestEventService.getEvent(responseFintEvent.getCorrId())
                .orElseThrow(() -> new NoRequestFoundException(responseFintEvent.getCorrId()));

        if (!responseFintEvent.getOrgId().equals(requestEvent.getOrgId())) {
            log.error("Recieved event response, did not match org-id: {}", responseFintEvent.getOrgId());
            throw new InvalidOrgIdException(responseFintEvent.getOrgId());
        }

        responseEventTopicProducer.sendEvent(responseFintEvent, requestEvent);

        entityProducerKafka.sendEntity(
                responseFintEvent.getOrgId(),
                requestEvent.getDomainName(),
                requestEvent.getPackageName(),
                requestEvent.getResourceName(),
                responseFintEvent.getValue(),
                responseFintEvent.getCorrId()
        );
    }
}
