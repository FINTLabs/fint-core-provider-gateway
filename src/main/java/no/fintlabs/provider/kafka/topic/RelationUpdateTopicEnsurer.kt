package no.fintlabs.provider.kafka.topic

import no.fintlabs.provider.config.ProviderProperties
import no.fintlabs.provider.config.RelationUpdateKafkaProperties
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
class RelationUpdateTopicEnsurer(
    private val entityTopicService: EntityTopicService,
    private val relationUpdateKafkaProperties: RelationUpdateKafkaProperties,
    private val metamodelService: MetamodelService,
    private val providerProperties: ProviderProperties
) {

    @EventListener(ApplicationReadyEvent::class)
    fun ensureRelationUpdateTopics() {
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
                        .resourceName("${component.domainName}-${component.packageName}-relation-update")
                        .build(),
                    EntityTopicConfiguration.stepBuilder()
                        .partitions(relationUpdateKafkaProperties.partitions)
                        .lastValueRetentionTime(relationUpdateKafkaProperties.retentionTime)
                        .nullValueRetentionTime(relationUpdateKafkaProperties.retentionTime)
                        .cleanupFrequency(EntityCleanupFrequency.NORMAL)
                        .build()
                )
            }
        }
    }
}
