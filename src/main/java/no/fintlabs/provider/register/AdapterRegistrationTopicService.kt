package no.fintlabs.provider.register

import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import no.fintlabs.kafka.entity.topic.EntityTopicService
import no.fintlabs.provider.kafka.TopicNamesConstants
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.function.Consumer

@Service
class AdapterRegistrationTopicService(
    private val entityTopicService: EntityTopicService
) {

    fun ensureCapabilityTopics(adapterContract: AdapterContract) {
        adapterContract.capabilities.forEach(Consumer { capability: AdapterCapability ->
            // TODO: Change retention time to be based on capability (Verify that Visma agrees with the latest contract)
            val retentionTime = Duration.ofDays(7).toMillis()

            val topicNameParameters = createTopicNameParameters(adapterContract.orgId, capability)
            entityTopicService.ensureTopic(topicNameParameters, retentionTime)
        })
    }

    private fun createTopicNameParameters(
        org: String,
        adapterCapability: AdapterCapability
    ) = EntityTopicNameParameters.builder()
        .orgId(org.replace(".", "-"))
        .domainContext(TopicNamesConstants.FINT_CORE)
        .resource(adapterCapability.toTopicResourceName())
        .build()

    private fun AdapterCapability.toTopicResourceName(): String = "$domainName-$packageName-$resourceName"
}
