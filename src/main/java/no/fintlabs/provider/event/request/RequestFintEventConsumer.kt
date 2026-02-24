package no.fintlabs.provider.event.request

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern
import no.fintlabs.kafka.event.EventConsumerConfiguration
import no.fintlabs.kafka.event.EventConsumerFactoryService
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters
import no.fintlabs.metamodel.MetamodelService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
open class RequestFintEventConsumer(
    private val eventConsumerFactoryService: EventConsumerFactoryService,
    private val requestEventService: RequestEventService,
    private val metamodelService: MetamodelService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    // The String is the corrId of the RequestFintEvent
    open fun requestFintEventListenerContainer(): ConcurrentMessageListenerContainer<String, RequestFintEvent> {
        return eventConsumerFactoryService.createFactory(
            RequestFintEvent::class.java,
            this::processEvent,
            EventConsumerConfiguration
                .builder()
                .seekingOffsetResetOnAssignment(true)
                .build()
        ).createContainer(
            EventTopicNamePatternParameters
                .builder()
                .orgId(FormattedTopicComponentPattern.any())
                .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                .eventName(ValidatedTopicComponentPattern.anyOf(*createEventNames()))
                .build()
        )
    }

    // Creates event name from component and resources names - utdanning-vurdering-elevfravar-request
    private fun createEventNames(): Array<String> =
        metamodelService.getComponents().flatMap { component ->
            component.resources.map { resource ->
                "${component.domainName}-${component.packageName}-${resource.name}-request"
            }
        }.toTypedArray()

    private fun processEvent(consumerRecord: ConsumerRecord<String?, RequestFintEvent>) {
        logger.info("RequestFintEvent received: {} - {}", consumerRecord.value().orgId, consumerRecord.value().corrId)
        requestEventService.addEvent(consumerRecord.value())
    }
}
