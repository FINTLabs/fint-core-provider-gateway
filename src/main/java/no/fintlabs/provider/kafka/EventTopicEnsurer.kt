package no.fintlabs.provider.kafka

import no.fintlabs.kafka.event.topic.EventTopicNameParameters
import no.fintlabs.kafka.event.topic.EventTopicService
import no.fintlabs.provider.config.ProviderProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.Map

@Component
class EventTopicEnsurer(
    private val providerProperties: ProviderProperties,
    private val eventTopicService: EventTopicService
) {
    @EventListener(ApplicationReadyEvent::class)
    fun ensureEventTopics() =
        Map.of<String, Long>(
            TopicNamesConstants.HEARTBEAT_EVENT_NAME, providerProperties.adapterHeartbeatRetentionTimeMs,
            TopicNamesConstants.ADAPTER_REGISTER_EVENT_NAME, providerProperties.adapterRegisterRetentionTimeMs,
            TopicNamesConstants.ADAPTER_FULL_SYNC_EVENT_NAME, providerProperties.adapterFullSyncRetentionTimeMs,
            TopicNamesConstants.ADAPTER_DELTA_SYNC_EVENT_NAME, providerProperties.adapterDeltaSyncRetentionTimeMs,
            TopicNamesConstants.ADAPTER_DELETE_SYNC_EVENT_NAME, providerProperties.adapterDeleteSyncRetentionTimeMs
        ).forEach { (eventName: String, retentionTime: Long) ->
            eventTopicService.ensureTopic(
                EventTopicNameParameters.builder()
                    .orgId(TopicNamesConstants.FINTLABS_NO)
                    .domainContext(TopicNamesConstants.FINT_CORE)
                    .eventName(eventName)
                    .build(),
                retentionTime
            )
        }
}