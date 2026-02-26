package no.fintlabs.provider.event.response;

import no.fintlabs.adapter.models.event.RequestFintEvent;
import no.fintlabs.adapter.models.event.ResponseFintEvent;
import no.fintlabs.provider.kafka.ProviderTopicService;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import no.novari.kafka.topic.name.TopicNamePrefixParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ResponseEventTopicProducer {

    private final ParameterizedTemplate<Object> eventProducer;
    private final ProviderTopicService topicService;

    public ResponseEventTopicProducer(ParameterizedTemplateFactory parameterizedTemplateFactory, ProviderTopicService topicService) {
        this.eventProducer = parameterizedTemplateFactory.createTemplate(Object.class);
        this.topicService = topicService;
    }

    public void sendEvent(ResponseFintEvent responseFintEvent, RequestFintEvent requestFintEvent) {
        EventTopicNameParameters topicNameParameters = EventTopicNameParameters
                .builder()
                .topicNamePrefixParameters(
                        TopicNamePrefixParameters
                                .stepBuilder()
                                .orgId(responseFintEvent.getOrgId().replace(".", "-"))
                                .domainContextApplicationDefault()
                                .build()
                )
                .eventName(createEventName(requestFintEvent))
                .build();

        if (!topicService.topicExists(topicNameParameters))
            topicService.createOrModifyTopic(topicNameParameters, Duration.ofDays(2));

        eventProducer.send(
                ParameterizedProducerRecord.builder()
                        .topicNameParameters(topicNameParameters)
                        .value(responseFintEvent)
                        .build()
        );
    }

    private String createEventName(RequestFintEvent requestFintEvent) {
        return "%s-%s-%s-response".formatted(
                requestFintEvent.getDomainName(),
                requestFintEvent.getPackageName(),
                requestFintEvent.getResourceName()
        );
    }

}
