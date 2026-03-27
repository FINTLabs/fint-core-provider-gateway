package no.fintlabs.provider.event.response

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.novari.kafka.producing.ParameterizedProducerRecord
import no.novari.kafka.producing.ParameterizedTemplateFactory
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.stereotype.Service

@Service
class ResponseFintEventProducer(
    eventProducerFactory: ParameterizedTemplateFactory
) {

    private val eventProducer = eventProducerFactory.createTemplate(ResponseFintEvent::class.java)

    fun sendEvent(responseFintEvent: ResponseFintEvent, requestFintEvent: RequestFintEvent) {
        eventProducer.send(
            ParameterizedProducerRecord.builder<ResponseFintEvent>()
                .topicNameParameters(requestFintEvent.toTopicNameParameters())
                .value(responseFintEvent)
                .build()
        )
    }

    private fun RequestFintEvent.toTopicNameParameters() =
        EventTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters.stepBuilder()
                    .orgId(orgId.replace(".", "-"))
                    .domainContextApplicationDefault()
                    .build()
            )
            .eventName("${domainName}-${packageName}-response")
            .build()
}
