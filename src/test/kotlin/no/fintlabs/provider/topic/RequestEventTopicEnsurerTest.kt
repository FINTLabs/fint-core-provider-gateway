package no.fintlabs.provider.topic

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.provider.config.ProducerProperties
import no.fintlabs.provider.config.ProviderProperties
import no.fintlabs.provider.kafka.topic.RequestEventTopicEnsurer
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import no.novari.metamodel.MetamodelService
import no.novari.metamodel.model.Component
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RequestEventTopicEnsurerTest {

    private lateinit var eventTopicService: EventTopicService
    private lateinit var metamodelService: MetamodelService
    private val requestProducerProperties = ProducerProperties()

    @BeforeEach
    fun setup() {
        eventTopicService = mockk()
        metamodelService = mockk()
        every { eventTopicService.createOrModifyTopic(any(), any()) } just Runs
    }

    private fun sut(orgIds: List<String> = listOf("fintlabs-no", "rogfk-no")) =
        RequestEventTopicEnsurer(
            eventTopicService,
            requestProducerProperties,
            metamodelService,
            ProviderProperties(orgIds = orgIds)
        )

    @Test
    fun `ensureRequestEventTopics creates a topic for each org-id and component combination`() {
        every { metamodelService.getComponents() } returns listOf(
            Component("utdanning", "elev"),
            Component("utdanning", "vurdering")
        )

        sut().ensureRequestEventTopics()

        verify(exactly = 4) { eventTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureRequestEventTopics uses correct event name with request suffix`() {
        every { metamodelService.getComponents() } returns listOf(Component("utdanning", "elev"))

        sut(orgIds = listOf("fintlabs-no")).ensureRequestEventTopics()

        val expected = EventTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters.stepBuilder()
                    .orgId("fintlabs-no")
                    .domainContextApplicationDefault()
                    .build()
            )
            .eventName("utdanning-elev-request")
            .build()

        verify(exactly = 1) { eventTopicService.createOrModifyTopic(expected, any()) }
    }

    @Test
    fun `ensureRequestEventTopics does nothing when org-ids list is empty`() {
        every { metamodelService.getComponents() } returns listOf(Component("utdanning", "elev"))

        sut(orgIds = emptyList()).ensureRequestEventTopics()

        verify(exactly = 0) { eventTopicService.createOrModifyTopic(any(), any()) }
    }
}
