package no.fintlabs.provider.kafka

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
class ResponseEventTopicEnsurer(
    private val eventTopicService: EventTopicService,
    private val responseProducerProperties: ProducerProperties,
    private val metamodelService: MetamodelService,
    private val providerProperties: ProviderProperties
) {

    @EventListener(ApplicationReadyEvent::class)
    fun ensureResponseEventTopics() {
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
                        .eventName("${component.domainName}-${component.packageName}-response")
                        .build(),
                    EventTopicConfiguration.stepBuilder()
                        .partitions(responseProducerProperties.partitions)
                        .retentionTime(responseProducerProperties.retentionTime)
                        .cleanupFrequency(EventCleanupFrequency.NORMAL)
                        .build()
                )
            }
        }
    }
}
