package no.fintlabs.provider.event.response;

import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.adapter.models.ResponseFintEvent;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

@Service
public class ResponseEventTopicProducer {

    private static final int RETENTION_TIME_MS = 172800000;

    private final EventProducer<Object> eventProducer;

    private final EventTopicService eventTopicService;

    public ResponseEventTopicProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        this.eventProducer = eventProducerFactory.createProducer(Object.class);
        this.eventTopicService = eventTopicService;
    }

    public void sendEvent(ResponseFintEvent responseFintEvent, RequestFintEvent requestFintEvent) {

        String eventName = "%s-%s-%s-%s-%s".formatted(
                requestFintEvent.getDomainName(),
                requestFintEvent.getPackageName(),
                requestFintEvent.getResourceName(),
                requestFintEvent.equals(OperationType.CREATE) ? "create" : "update",
                "response");

        EventTopicNameParameters topicNameParameters = EventTopicNameParameters
                .builder()
                .orgId(responseFintEvent.getOrgId())
                .domainContext("fint-core")
                .eventName(eventName)
                .build();

        eventTopicService.ensureTopic(topicNameParameters, RETENTION_TIME_MS);

        eventProducer.send(
                EventProducerRecord.builder()
                        .topicNameParameters(topicNameParameters)
                        .value(responseFintEvent)
                        .build()
        );
    }

    public enum OperationType {
        CREATE,
        UPDATE
    }
}
