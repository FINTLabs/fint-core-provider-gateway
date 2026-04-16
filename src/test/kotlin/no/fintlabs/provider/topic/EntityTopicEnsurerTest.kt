package no.fintlabs.provider.topic

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.provider.config.ComponentConfig
import no.fintlabs.provider.config.EntityKafkaProperties
import no.fintlabs.provider.config.ProviderProperties
import no.fintlabs.provider.kafka.topic.EntityTopicEnsurer
import no.novari.kafka.topic.EntityTopicService
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EntityTopicEnsurerTest {

    private lateinit var entityTopicService: EntityTopicService
    private val entityKafkaProperties = EntityKafkaProperties()

    @BeforeEach
    fun setup() {
        entityTopicService = mockk()
        every { entityTopicService.createOrModifyTopic(any(), any()) } just Runs
    }

    private fun sut(components: List<ComponentConfig> = emptyList()) =
        EntityTopicEnsurer(
            entityTopicService,
            entityKafkaProperties,
            ProviderProperties(components = components)
        )

    @Test
    fun `ensureEntityTopics creates a topic for each org-id and component combination`() {
        val components = listOf(
            ComponentConfig(domainName = "utdanning", "elev", listOf("fintlabs-no", "rogfk-no")),
            ComponentConfig(domainName = "utdanning", "vurdering", listOf("fintlabs-no"))
        )

        sut(components).ensureEntityTopics()

        verify(exactly = 3) { entityTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureEntityTopics uses resourceName combining domain and packageName`() {
        val components = listOf(
            ComponentConfig(domainName = "utdanning", "elev", listOf("fintlabs-no"))
        )

        sut(components).ensureEntityTopics()

        val expected = EntityTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters.stepBuilder()
                    .orgId("fintlabs-no")
                    .domainContextApplicationDefault()
                    .build()
            )
            .resourceName("utdanning-elev")
            .build()

        verify(exactly = 1) { entityTopicService.createOrModifyTopic(expected, any()) }
    }

    @Test
    fun `ensureEntityTopics does nothing when components list is empty`() {
        sut(emptyList()).ensureEntityTopics()

        verify(exactly = 0) { entityTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureEntityTopics does nothing when component has no org-ids`() {
        val components = listOf(
            ComponentConfig(domainName = "utdanning", "elev", emptyList())
        )

        sut(components).ensureEntityTopics()

        verify(exactly = 0) { entityTopicService.createOrModifyTopic(any(), any()) }
    }
}
