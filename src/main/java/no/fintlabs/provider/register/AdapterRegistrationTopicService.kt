package no.fintlabs.provider.register

import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.provider.config.EntityKafkaProperties
import no.novari.kafka.topic.EntityTopicService
import no.novari.kafka.topic.configuration.EntityCleanupFrequency
import no.novari.kafka.topic.configuration.EntityTopicConfiguration
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.stereotype.Service
import java.util.function.Consumer

@Service
class AdapterRegistrationTopicService(
    private val entityTopicService: EntityTopicService,
    private val entityKafkaProperties: EntityKafkaProperties,
) {

    fun ensureCapabilityTopics(adapterContract: AdapterContract) {
        adapterContract.capabilities.forEach(Consumer { capability: AdapterCapability ->
            // TODO: Change retention time to be based on capability (Verify that Visma agrees with the latest contract)
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
        })
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
        .resourceName(adapterCapability.toTopicResourceName())
        .build();

    private fun AdapterCapability.toTopicResourceName(): String = "$domainName-$packageName-$resourceName"
}
