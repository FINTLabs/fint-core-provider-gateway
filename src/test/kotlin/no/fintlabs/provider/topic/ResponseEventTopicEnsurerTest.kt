package no.fintlabs.provider.topic

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.provider.config.ComponentConfig
import no.fintlabs.provider.config.ProducerProperties
import no.fintlabs.provider.config.ProviderProperties
import no.fintlabs.provider.kafka.topic.ResponseEventTopicEnsurer
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResponseEventTopicEnsurerTest {

    private lateinit var eventTopicService: EventTopicService
    private val responseProducerProperties = ProducerProperties()

    @BeforeEach
    fun setup() {
        eventTopicService = mockk()
        every { eventTopicService.createOrModifyTopic(any(), any()) } just Runs
    }

    private fun sut(components: List<ComponentConfig> = emptyList()) =
        ResponseEventTopicEnsurer(
            eventTopicService,
            responseProducerProperties,
            ProviderProperties(components = components)
        )

    @Test
    fun `ensureResponseEventTopics creates a topic for each org-id and component combination`() {
        val components = listOf(
            ComponentConfig(domainName = "utdanning", "elev", listOf("fintlabs-no", "rogfk-no")),
            ComponentConfig(domainName = "utdanning", "vurdering", listOf("fintlabs-no"))
        )

        sut(components).ensureResponseEventTopics()

        verify(exactly = 3) { eventTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureResponseEventTopics uses correct event name with response suffix`() {
        val components = listOf(
            ComponentConfig(domainName = "utdanning", "elev", listOf("fintlabs-no"))
        )

        sut(components).ensureResponseEventTopics()

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
    fun `ensureResponseEventTopics does nothing when components list is empty`() {
        sut(emptyList()).ensureResponseEventTopics()

        verify(exactly = 0) { eventTopicService.createOrModifyTopic(any(), any()) }
    }
}
