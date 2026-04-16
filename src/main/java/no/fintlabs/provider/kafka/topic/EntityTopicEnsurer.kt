package no.fintlabs.provider.kafka.topic

import no.fintlabs.provider.config.EntityKafkaProperties
import no.fintlabs.provider.config.ProviderProperties
import no.novari.kafka.topic.EntityTopicService
import no.novari.kafka.topic.configuration.EntityCleanupFrequency
import no.novari.kafka.topic.configuration.EntityTopicConfiguration
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import no.novari.metamodel.MetamodelService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "fint.provider", name = ["ensure-topics"], havingValue = "true")
class EntityTopicEnsurer(
    private val entityTopicService: EntityTopicService,
    private val entityKafkaProperties: EntityKafkaProperties,
    private val metamodelService: MetamodelService,
    private val providerProperties: ProviderProperties
) {

    @EventListener(ApplicationReadyEvent::class)
    fun ensureEntityTopics() {
        providerProperties.orgIds.forEach { orgId ->
            metamodelService.getComponents().forEach { component ->
                entityTopicService.createOrModifyTopic(
                    EntityTopicNameParameters.builder()
                        .topicNamePrefixParameters(
                            TopicNamePrefixParameters.stepBuilder()
                                .orgId(orgId)
                                .domainContextApplicationDefault()
                                .build()
                        )
                        .resourceName("${component.domainName}-${component.packageName}")
                        .build(),
                    EntityTopicConfiguration.stepBuilder()
                        .partitions(entityKafkaProperties.partitions)
                        .lastValueRetentionTime(entityKafkaProperties.retentionTime)
                        .nullValueRetentionTime(entityKafkaProperties.retentionTime)
                        .cleanupFrequency(EntityCleanupFrequency.NORMAL)
                        .build()
                )
            }
        }
    }
}
