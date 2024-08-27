package no.fintlabs.provider.event.response;

import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.provider.kafka.ProviderTopicService;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ResponseEventTopicProducer {

    private final EventProducer<Object> eventProducer;
    private final ProviderTopicService topicService;

    public ResponseEventTopicProducer(EventProducerFactory eventProducerFactory, ProviderTopicService topicService) {
        this.eventProducer = eventProducerFactory.createProducer(Object.class);
        this.topicService = topicService;
    }

    public void sendEvent(ResponseFintEvent responseFintEvent, RequestFintEvent requestFintEvent) {
        EventTopicNameParameters topicNameParameters = EventTopicNameParameters
                .builder()
                .orgId(responseFintEvent.getOrgId())
                .domainContext("fint-core")
                .eventName(createEventName(requestFintEvent))
                .build();

        if (!topicService.topicExists(topicNameParameters))
            topicService.ensureTopic(topicNameParameters, Duration.ofDays(2).toMillis());

        eventProducer.send(
                EventProducerRecord.builder()
                        .topicNameParameters(topicNameParameters)
                        .value(responseFintEvent)
                        .build()
        );
    }

    private String createEventName(RequestFintEvent requestFintEvent) {
        return "%s-%s-%s-%s-response".formatted(
                requestFintEvent.getDomainName(),
                requestFintEvent.getPackageName(),
                requestFintEvent.getResourceName(),
                requestFintEvent.getOperationType().toString().toLowerCase()
        );
    }

}
