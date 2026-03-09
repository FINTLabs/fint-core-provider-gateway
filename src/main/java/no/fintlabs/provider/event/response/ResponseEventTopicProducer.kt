package no.fintlabs.provider.event.response

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.kafka.event.EventProducerFactory
import no.fintlabs.kafka.event.EventProducerRecord
import no.fintlabs.kafka.event.topic.EventTopicNameParameters
import no.fintlabs.kafka.event.topic.EventTopicService
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class ResponseEventTopicProducer(
    eventProducerFactory: EventProducerFactory,
    private val eventTopicService: EventTopicService
) {

    private val eventProducer = eventProducerFactory.createProducer(ResponseFintEvent::class.java)

    fun sendEvent(responseFintEvent: ResponseFintEvent, requestFintEvent: RequestFintEvent) {
        val topicNameParameters = EventTopicNameParameters
            .builder()
            .orgId(responseFintEvent.orgId)
            .domainContext("fint-core")
            .eventName(createEventName(requestFintEvent))
            .build()

        eventTopicService.ensureTopic(
            topicNameParameters,
            Duration.ofDays(2).toMillis()
        )

        eventProducer.send(
            EventProducerRecord.builder<ResponseFintEvent>()
                .topicNameParameters(topicNameParameters)
                .value(responseFintEvent)
                .build()
        )
    }

    private fun createEventName(requestFintEvent: RequestFintEvent): String =
        "${requestFintEvent.domainName}-${requestFintEvent.packageName}-${requestFintEvent.resourceName}-response"

}
