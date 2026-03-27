package no.fintlabs.provider.kafka

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.provider.config.EntityKafkaProperties
import no.fintlabs.provider.config.ProviderProperties
import no.novari.kafka.topic.EntityTopicService
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import no.novari.metamodel.MetamodelService
import no.novari.metamodel.model.Component
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EntityTopicEnsurerTest {

    private lateinit var entityTopicService: EntityTopicService
    private lateinit var metamodelService: MetamodelService
    private val entityKafkaProperties = EntityKafkaProperties()

    @BeforeEach
    fun setup() {
        entityTopicService = mockk()
        metamodelService = mockk()
        every { entityTopicService.createOrModifyTopic(any(), any()) } just Runs
    }

    private fun sut(orgIds: List<String> = listOf("fintlabs-no", "rogfk-no")) =
        EntityTopicEnsurer(entityTopicService, entityKafkaProperties, metamodelService, ProviderProperties(orgIds = orgIds))

    @Test
    fun `ensureEntityTopics creates a topic for each org-id and component combination`() {
        every { metamodelService.getComponents() } returns listOf(
            Component("utdanning", "elev"),
            Component("utdanning", "vurdering")
        )

        sut().ensureEntityTopics()

        verify(exactly = 4) { entityTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureEntityTopics uses resourceName combining domainName and packageName`() {
        every { metamodelService.getComponents() } returns listOf(Component("utdanning", "elev"))

        sut(orgIds = listOf("fintlabs-no")).ensureEntityTopics()

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
    fun `ensureEntityTopics does nothing when org-ids list is empty`() {
        every { metamodelService.getComponents() } returns listOf(Component("utdanning", "elev"))

        sut(orgIds = emptyList()).ensureEntityTopics()

        verify(exactly = 0) { entityTopicService.createOrModifyTopic(any(), any()) }
    }
}
