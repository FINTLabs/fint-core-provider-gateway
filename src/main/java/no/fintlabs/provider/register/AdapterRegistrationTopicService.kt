package no.fintlabs.provider.register

import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.provider.config.EntityKafkaProperties
import no.novari.kafka.topic.EntityTopicService
import no.novari.kafka.topic.configuration.EntityCleanupFrequency
import no.novari.kafka.topic.configuration.EntityTopicConfiguration
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AdapterRegistrationTopicService(
    private val entityTopicService: EntityTopicService,
    private val entityKafkaProperties: EntityKafkaProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun createCapabilityTopics(adapterContract: AdapterContract) {
        adapterContract.capabilities
            .distinctBy { it.component }
            .forEach { capability ->
                if (logger.isDebugEnabled) {
                    logger.debug(
                        "Ensuring entity-topic for capability: {} with partitions: {}",
                        capability.component,
                        entityKafkaProperties.partitions
                    )
                }
                entityTopicService.createOrModifyTopic(
                    createTopicNameParameters(adapterContract.orgId, capability),
                    EntityTopicConfiguration
                        .stepBuilder()
                        .partitions(entityKafkaProperties.partitions)
                        .lastValueRetentionTime(entityKafkaProperties.retentionTime)
                        .nullValueRetentionTime(entityKafkaProperties.retentionTime)
                        .cleanupFrequency(EntityCleanupFrequency.NORMAL)
                        .build()
                )
            }

    }

    private fun createTopicNameParameters(
        org: String,
        adapterCapability: AdapterCapability
    ) = EntityTopicNameParameters.builder()
        .topicNamePrefixParameters(
            TopicNamePrefixParameters
                .stepBuilder()
                .orgId(org.replace(".", "-"))
                .domainContextApplicationDefault()
                .build()
        )
        .resourceName(adapterCapability.component)
        .build();

}
