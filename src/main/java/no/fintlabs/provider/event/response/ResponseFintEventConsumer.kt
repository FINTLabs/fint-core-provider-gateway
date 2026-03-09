package no.fintlabs.provider.event.response

import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern
import no.fintlabs.kafka.event.EventConsumerConfiguration
import no.fintlabs.kafka.event.EventConsumerFactoryService
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters
import no.fintlabs.provider.config.KafkaConfig
import no.fintlabs.provider.event.request.RequestEventService
import no.novari.metamodel.MetamodelService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.stereotype.Service

@Service
class ResponseFintEventConsumer(
    private val eventConsumerFactoryService: EventConsumerFactoryService,
    private val requestEventService: RequestEventService,
    private val metamodelService: MetamodelService,
    private val kafkaConfig: KafkaConfig,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun registerResponseFintEventListener(): ConcurrentMessageListenerContainer<String?, ResponseFintEvent> =
        eventConsumerFactoryService.createFactory(
            ResponseFintEvent::class.java,
            this::processEvent,
            EventConsumerConfiguration
                .builder()
                .seekingOffsetResetOnAssignment(true)
                .groupIdSuffix(kafkaConfig.groupIdSuffix)
                .build()
        ).createContainer(
            EventTopicNamePatternParameters
                .builder()
                .orgId(FormattedTopicComponentPattern.any())
                .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                .eventName(ValidatedTopicComponentPattern.anyOf(*createEventNames()))
                .build()
        )

    private fun createEventNames(): Array<String> =
        metamodelService.getComponents().flatMap { component ->
            component.resources.map { resource ->
                "${component.domainName}-${component.packageName}-${resource.name}-response"
            }
        }.toTypedArray()

    private fun processEvent(consumerRecord: ConsumerRecord<String?, ResponseFintEvent>) {
        logger.info("ResponseFintEvent received: {} - {}", consumerRecord.value().orgId, consumerRecord.value().corrId)
        requestEventService.removeEvent(consumerRecord.value().corrId)
    }
}
