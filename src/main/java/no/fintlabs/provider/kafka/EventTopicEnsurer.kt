package no.fintlabs.provider.kafka

import no.fintlabs.provider.config.ProviderProperties
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.configuration.EventCleanupFrequency
import no.novari.kafka.topic.configuration.EventTopicConfiguration
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class EventTopicEnsurer(
    private val providerProperties: ProviderProperties,
    private val eventTopicService: EventTopicService
) {
    @EventListener(ApplicationReadyEvent::class)
    fun ensureEventTopics() = with(providerProperties.kafka.adapter) {
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
                    .partitions(providerProperties.kafka.entityPartitions)
                    .retentionTime(retentionTime)
                    .cleanupFrequency(EventCleanupFrequency.NORMAL)
                    .build()
            )
        }
    }
}