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
import no.novari.metamodel.MetamodelService
import no.novari.metamodel.model.Component
import org.springframework.stereotype.Service


@Service
class ResponseFintEventProducer(
    eventProducerFactory: ParameterizedTemplateFactory,
    private val eventTopicService: EventTopicService,
    private val responseProducerProperties: ProducerProperties,
    private val metamodelService: MetamodelService
) {

    init {
        listOf(
            "afk-no",
            "agderfk-no",
            "bfk-no",
            "ffk-no",
            "fintlabs-no",
            "innlandetfylke-no",
            "mrfylke-no",
            "nfk-no",
            "ofk-no",
            "rogfk-no",
            "telemarkfylke-no",
            "tromsfylke-no",
            "trondelagfylke-no",
            "ude-oslo-kommune-no",
            "vestfoldfylke-no",
            "visma-com",
            "vlfk-no"
        ).forEach { orgId ->
            metamodelService.getComponents().forEach { component ->
                eventTopicService.createOrModifyTopic(
                    component.toTopicNameParameters(orgId),
                    EventTopicConfiguration.stepBuilder()
                        .partitions(responseProducerProperties.partitions)
                        .retentionTime(responseProducerProperties.retentionTime)
                        .cleanupFrequency(EventCleanupFrequency.NORMAL)
                        .build()
                )
            }

        }
    }

    private val eventProducer = eventProducerFactory.createTemplate(ResponseFintEvent::class.java)

    fun sendEvent(responseFintEvent: ResponseFintEvent, requestFintEvent: RequestFintEvent) {
        val topicNameParameters = requestFintEvent.toTopicNameParameters()

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
            .eventName("${domainName}-${packageName}-response")
            .build()

    private fun Component.toTopicNameParameters(orgId: String) =
        EventTopicNameParameters
            .builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters
                    .stepBuilder()
                    .orgId(orgId)
                    .domainContextApplicationDefault()
                    .build()
            )
            .eventName("${domainName}-${packageName}-response")
            .build()

}
