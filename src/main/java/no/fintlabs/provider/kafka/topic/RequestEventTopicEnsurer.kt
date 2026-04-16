package no.fintlabs.provider.kafka.topic

import no.fintlabs.provider.config.ProducerProperties
import no.fintlabs.provider.config.ProviderProperties
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.configuration.EventCleanupFrequency
import no.novari.kafka.topic.configuration.EventTopicConfiguration
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import no.novari.metamodel.MetamodelService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "fint.provider", name = ["ensure-topics"], havingValue = "true")
class RequestEventTopicEnsurer(
    private val eventTopicService: EventTopicService,
    private val requestProducerProperties: ProducerProperties,
    private val metamodelService: MetamodelService,
    private val providerProperties: ProviderProperties
) {

    @EventListener(ApplicationReadyEvent::class)
    fun ensureRequestEventTopics() {
        providerProperties.orgIds.forEach { orgId ->
            metamodelService.getComponents().forEach { component ->
                eventTopicService.createOrModifyTopic(
                    EventTopicNameParameters.builder()
                        .topicNamePrefixParameters(
                            TopicNamePrefixParameters.stepBuilder()
                                .orgId(orgId)
                                .domainContextApplicationDefault()
                                .build()
                        )
                        .eventName("${component.domainName}-${component.packageName}-request")
                        .build(),
                    EventTopicConfiguration.stepBuilder()
                        .partitions(requestProducerProperties.partitions)
                        .retentionTime(requestProducerProperties.retentionTime)
                        .cleanupFrequency(EventCleanupFrequency.NORMAL)
                        .build()
                )
            }
        }
    }
}
