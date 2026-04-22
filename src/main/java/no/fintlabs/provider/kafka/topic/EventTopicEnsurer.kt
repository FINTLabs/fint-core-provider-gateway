package no.fintlabs.provider.kafka.topic

import no.fintlabs.provider.config.AdapterKafkaProperties
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.configuration.EventCleanupFrequency
import no.novari.kafka.topic.configuration.EventTopicConfiguration
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "fint.provider", name = ["ensure-topics"], havingValue = "true", matchIfMissing = true)
class EventTopicEnsurer(
    private val adapterKafkaProperties: AdapterKafkaProperties,
    private val eventTopicService: EventTopicService
) {
    @EventListener(ApplicationReadyEvent::class)
    fun ensureEventTopics() = with(adapterKafkaProperties) {
        listOf(
            TopicNamesConstants.HEARTBEAT_EVENT_NAME to heartbeatRetentionTime,
            TopicNamesConstants.ADAPTER_REGISTER_EVENT_NAME to registerRetentionTime,
            TopicNamesConstants.ADAPTER_FULL_SYNC_EVENT_NAME to fullSyncRetentionTime,
            TopicNamesConstants.ADAPTER_DELTA_SYNC_EVENT_NAME to deltaSyncRetentionTime,
            TopicNamesConstants.ADAPTER_DELETE_SYNC_EVENT_NAME to deleteSyncRetentionTime
        ).forEach { (eventName, retentionTime) ->
            eventTopicService.createOrModifyTopic(
                EventTopicNameParameters.builder()
                    .topicNamePrefixParameters(
                        TopicNamePrefixParameters.stepBuilder()
                            .orgIdApplicationDefault()
                            .domainContextApplicationDefault()
                            .build()
                    )
                    .eventName(eventName)
                    .build(),
                EventTopicConfiguration
                    .stepBuilder()
                    .partitions(partitions)
                    .retentionTime(retentionTime)
                    .cleanupFrequency(EventCleanupFrequency.NORMAL)
                    .build()
            )
        }
    }
}
