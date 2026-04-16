package no.fintlabs.provider.topic

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.provider.config.ProviderProperties
import no.fintlabs.provider.config.RelationUpdateKafkaProperties
import no.fintlabs.provider.kafka.topic.RelationUpdateTopicEnsurer
import no.novari.kafka.topic.EntityTopicService
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import no.novari.metamodel.MetamodelService
import no.novari.metamodel.model.Component
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RelationUpdateTopicEnsurerTest {

    private lateinit var entityTopicService: EntityTopicService
    private lateinit var metamodelService: MetamodelService
    private val relationUpdateKafkaProperties = RelationUpdateKafkaProperties()

    @BeforeEach
    fun setup() {
        entityTopicService = mockk()
        metamodelService = mockk()
        every { entityTopicService.createOrModifyTopic(any(), any()) } just Runs
    }

    private fun sut(orgIds: List<String> = listOf("fintlabs-no", "rogfk-no")) =
        RelationUpdateTopicEnsurer(
            entityTopicService,
            relationUpdateKafkaProperties,
            metamodelService,
            ProviderProperties(orgIds = orgIds)
        )

    @Test
    fun `ensureRelationUpdateTopics creates a topic for each org-id and component combination`() {
        every { metamodelService.getComponents() } returns listOf(
            Component("utdanning", "elev"),
            Component("utdanning", "vurdering")
        )

        sut().ensureRelationUpdateTopics()

        verify(exactly = 4) { entityTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureRelationUpdateTopics uses resourceName with relation-update suffix`() {
        every { metamodelService.getComponents() } returns listOf(Component("utdanning", "elev"))

        sut(orgIds = listOf("fintlabs-no")).ensureRelationUpdateTopics()

        val expected = EntityTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters.stepBuilder()
                    .orgId("fintlabs-no")
                    .domainContextApplicationDefault()
                    .build()
            )
            .resourceName("utdanning-elev-relation-update")
            .build()

        verify(exactly = 1) { entityTopicService.createOrModifyTopic(expected, any()) }
    }

    @Test
    fun `ensureRelationUpdateTopics does nothing when org-ids list is empty`() {
        every { metamodelService.getComponents() } returns listOf(Component("utdanning", "elev"))

        sut(orgIds = emptyList()).ensureRelationUpdateTopics()

        verify(exactly = 0) { entityTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureRelationUpdateTopics defaults to 6 partitions`() {
        org.junit.jupiter.api.Assertions.assertEquals(6, relationUpdateKafkaProperties.partitions)
    }
}
