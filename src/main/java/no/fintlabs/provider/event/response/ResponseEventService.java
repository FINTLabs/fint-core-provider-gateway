package no.fintlabs.provider.event.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.provider.datasync.EntityProducerKafka;
import no.fintlabs.provider.event.request.RequestEventService;
import no.fintlabs.provider.exception.InvalidOrgIdException;
import no.fintlabs.provider.exception.InvalidSyncPageEntryException;
import no.fintlabs.provider.exception.NoRequestFoundException;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINT_CORE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseEventService {

    private final ResponseEventTopicProducer responseEventTopicProducer;
    private final RequestEventService requestEventService;
    private final EntityProducerKafka entityProducerKafka;

    public void handleEvent(ResponseFintEvent responseFintEvent) throws NoRequestFoundException, InvalidOrgIdException {
        RequestFintEvent requestEvent = requestEventService.getEvent(responseFintEvent.getCorrId())
                .orElseThrow(() -> new NoRequestFoundException(responseFintEvent.getCorrId()));

        if (!responseFintEvent.getOrgId().equals(requestEvent.getOrgId())) {
            log.error("Recieved event response, did not match request org-id: {}", responseFintEvent.getOrgId());
            throw new InvalidOrgIdException(responseFintEvent.getOrgId());
        }

        if (responseFintEvent.getValue() == null) {
            log.error("Recieved a SyncPageEntry that is null");
            throw new InvalidSyncPageEntryException("SyncPageEntry is null");
        }

        responseEventTopicProducer.sendEvent(responseFintEvent, requestEvent);

        entityProducerKafka.sendEntity(
                EntityTopicNameParameters.builder()
                        .orgId(responseFintEvent.getOrgId())
                        .domainContext(FINT_CORE)
                        .resource("%s-%s-%s".formatted(requestEvent.getDomainName(), requestEvent.getPackageName(), requestEvent.getResourceName()))
                        .build(),
                responseFintEvent.getValue(),
                responseFintEvent.getCorrId()
        );
    }
}
