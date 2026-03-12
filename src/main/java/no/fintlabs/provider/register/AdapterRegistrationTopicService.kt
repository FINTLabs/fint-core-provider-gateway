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
import java.util.concurrent.ConcurrentHashMap

@Service
class AdapterRegistrationTopicService(
    private val entityTopicService: EntityTopicService,
    private val entityKafkaProperties: EntityKafkaProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val ensuredTopics = ConcurrentHashMap.newKeySet<String>()

    /**
     * Ensures a Kafka entity topic exists for each unique component (domain-package combination)
     * in the adapter contract. Topics are created per org, and if a topic has already been
     * ensured for a given org and component in this runtime, it is skipped.
     *
     * @param adapterContract the contract containing the org and its capabilities
     */
    fun createCapabilityTopics(adapterContract: AdapterContract) {
        adapterContract.capabilities
            .distinctBy { it.component }
            .forEach { ensureTopicExists(adapterContract.orgId, it) }
    }

    private fun ensureTopicExists(orgId: String, capability: AdapterCapability) {
        val topicKey = "$orgId:${capability.component}"
        if (!ensuredTopics.add(topicKey)) return

        logger.debug(
            "Ensuring entity-topic for org: {} component: {} with partitions: {}",
            orgId,
            capability.component,
            entityKafkaProperties.partitions
        )

        entityTopicService.createOrModifyTopic(
            createTopicNameParameters(orgId, capability),
            EntityTopicConfiguration
                .stepBuilder()
                .partitions(entityKafkaProperties.partitions)
                .lastValueRetentionTime(entityKafkaProperties.retentionTime)
                .nullValueRetentionTime(entityKafkaProperties.retentionTime)
                .cleanupFrequency(EntityCleanupFrequency.NORMAL)
                .build()
        )
    }

    private fun createTopicNameParameters(orgId: String, capability: AdapterCapability) =
        EntityTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters
                    .stepBuilder()
                    .orgId(orgId.replace(".", "-"))
                    .domainContextApplicationDefault()
                    .build()
            )
            .resourceName(capability.component)
            .build()
}
