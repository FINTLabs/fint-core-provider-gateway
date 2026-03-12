package no.fintlabs.provider.event.response

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.provider.config.ProducerProperties
import no.novari.kafka.producing.ParameterizedProducerRecord
import no.novari.kafka.producing.ParameterizedTemplateFactory
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.configuration.EventCleanupFrequency
import no.novari.kafka.topic.configuration.EventTopicConfiguration
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.stereotype.Service


@Service
class ResponseEventTopicProducer(
    eventProducerFactory: ParameterizedTemplateFactory,
    private val eventTopicService: EventTopicService,
    private val responseProducerProperties: ProducerProperties
) {

    private val eventProducer = eventProducerFactory.createTemplate(ResponseFintEvent::class.java)

    fun sendEvent(responseFintEvent: ResponseFintEvent, requestFintEvent: RequestFintEvent) {
        val topicNameParameters = requestFintEvent.toTopicNameParameters()

        eventTopicService.createOrModifyTopic(
            topicNameParameters,
            EventTopicConfiguration.stepBuilder()
                .partitions(responseProducerProperties.partitions)
                .retentionTime(responseProducerProperties.retentionTime)
                .cleanupFrequency(EventCleanupFrequency.NORMAL)
                .build()
        )

        eventProducer.send(
            ParameterizedProducerRecord.builder<ResponseFintEvent>()
                .topicNameParameters(topicNameParameters)
                .value(responseFintEvent)
                .build()
        )
    }

    private fun RequestFintEvent.toTopicNameParameters() =
        EventTopicNameParameters
            .builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters
                    .stepBuilder()
                    .orgId(orgId.replace(".", "-"))
                    .domainContextApplicationDefault()
                    .build()
            )
            .eventName(toTopicEventName())
            .build()

    private fun RequestFintEvent.toTopicEventName(): String = "${domainName}-${packageName}-response"

}
