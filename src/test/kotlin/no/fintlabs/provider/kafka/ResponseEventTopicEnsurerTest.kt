package no.fintlabs.provider.kafka

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.provider.config.ProducerProperties
import no.fintlabs.provider.config.ProviderProperties
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import no.novari.metamodel.MetamodelService
import no.novari.metamodel.model.Component
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResponseEventTopicEnsurerTest {

    private lateinit var eventTopicService: EventTopicService
    private lateinit var metamodelService: MetamodelService
    private val responseProducerProperties = ProducerProperties()

    @BeforeEach
    fun setup() {
        eventTopicService = mockk()
        metamodelService = mockk()
        every { eventTopicService.createOrModifyTopic(any(), any()) } just Runs
    }

    private fun sut(orgIds: List<String> = listOf("fintlabs-no", "rogfk-no")) =
        ResponseEventTopicEnsurer(eventTopicService, responseProducerProperties, metamodelService, ProviderProperties(orgIds = orgIds))

    @Test
    fun `ensureResponseEventTopics creates a topic for each org-id and component combination`() {
        every { metamodelService.getComponents() } returns listOf(
            Component("utdanning", "elev"),
            Component("utdanning", "vurdering")
        )

        sut().ensureResponseEventTopics()

        verify(exactly = 4) { eventTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureResponseEventTopics uses correct event name with response suffix`() {
        every { metamodelService.getComponents() } returns listOf(Component("utdanning", "elev"))

        sut(orgIds = listOf("fintlabs-no")).ensureResponseEventTopics()

        val expected = EventTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters.stepBuilder()
                    .orgId("fintlabs-no")
                    .domainContextApplicationDefault()
                    .build()
            )
            .eventName("utdanning-elev-response")
            .build()

        verify(exactly = 1) { eventTopicService.createOrModifyTopic(expected, any()) }
    }

    @Test
    fun `ensureResponseEventTopics does nothing when org-ids list is empty`() {
        every { metamodelService.getComponents() } returns listOf(Component("utdanning", "elev"))

        sut(orgIds = emptyList()).ensureResponseEventTopics()

        verify(exactly = 0) { eventTopicService.createOrModifyTopic(any(), any()) }
    }
}
