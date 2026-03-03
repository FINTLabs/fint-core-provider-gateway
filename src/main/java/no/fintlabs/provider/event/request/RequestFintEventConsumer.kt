package no.fintlabs.provider.event.request

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.provider.config.KafkaConfig
import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService
import no.novari.kafka.topic.name.EventTopicNamePatternParameters
import no.novari.kafka.topic.name.TopicNamePatternParameterPattern
import no.novari.kafka.topic.name.TopicNamePatternPrefixParameters
import no.novari.metamodel.MetamodelService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
open class RequestFintEventConsumer(
    private val parameterizedListenerContainerFactoryService: ParameterizedListenerContainerFactoryService,
    private val errorHandlerFactory: ErrorHandlerFactory,
    private val requestEventService: RequestEventService,
    private val metamodelService: MetamodelService,
    private val kafkaConfig: KafkaConfig,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    open fun requestFintEventListenerContainer(): ConcurrentMessageListenerContainer<String, RequestFintEvent> {
        return parameterizedListenerContainerFactoryService.createRecordListenerContainerFactory(
            RequestFintEvent::class.java,
            this::processEvent,
            ListenerConfiguration
                .stepBuilder()
                .groupIdApplicationDefaultWithSuffix(kafkaConfig.getGroupIdSuffix())
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                .seekToBeginningOnAssignment()
                .build(),
            errorHandlerFactory.createErrorHandler(
                ErrorHandlerConfiguration
                    .stepBuilder<RequestFintEvent>()
                    .noRetries()
                    .skipFailedRecords()
                    .build()
            )
        ).createContainer(
            EventTopicNamePatternParameters
                .builder()
                .topicNamePatternPrefixParameters(
                    TopicNamePatternPrefixParameters
                        .stepBuilder()
                        .orgId(TopicNamePatternParameterPattern.any())
                        .domainContextApplicationDefault()
                        .build()
                )
                .eventName(TopicNamePatternParameterPattern.anyOf(*createEventNames()))
                .build()
        )
    }

    private fun createEventNames(): Array<String> =
        metamodelService.getResources()
            .map { "${it.name}-request" }
            .toTypedArray()

    private fun processEvent(consumerRecord: ConsumerRecord<String, RequestFintEvent>) {
        logger.info("RequestFintEvent received: {} - {}", consumerRecord.value().orgId, consumerRecord.value().corrId)
        requestEventService.addEvent(consumerRecord.value())
    }
}
