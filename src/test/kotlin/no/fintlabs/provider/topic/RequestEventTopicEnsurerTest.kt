package no.fintlabs.provider.topic

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.provider.config.ComponentConfig
import no.fintlabs.provider.config.ProducerProperties
import no.fintlabs.provider.config.ProviderProperties
import no.fintlabs.provider.kafka.topic.RequestEventTopicEnsurer
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RequestEventTopicEnsurerTest {

    private lateinit var eventTopicService: EventTopicService
    private val requestProducerProperties = ProducerProperties()

    @BeforeEach
    fun setup() {
        eventTopicService = mockk()
        every { eventTopicService.createOrModifyTopic(any(), any()) } just Runs
    }

    private fun sut(components: List<ComponentConfig> = emptyList()) =
        RequestEventTopicEnsurer(
            eventTopicService,
            requestProducerProperties,
            ProviderProperties(components = components)
        )

    @Test
    fun `ensureRequestEventTopics creates a topic for each org-id and component combination`() {
        val components = listOf(
            ComponentConfig(domainName = "utdanning", "elev", listOf("fintlabs-no", "rogfk-no")),
            ComponentConfig(domainName = "utdanning", "vurdering", listOf("fintlabs-no"))
        )

        sut(components).ensureRequestEventTopics()

        verify(exactly = 3) { eventTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureRequestEventTopics uses correct event name with request suffix`() {
        val components = listOf(
            ComponentConfig(domainName = "utdanning", "elev", listOf("fintlabs-no"))
        )

        sut(components).ensureRequestEventTopics()

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
    fun `ensureRequestEventTopics does nothing when components list is empty`() {
        sut(emptyList()).ensureRequestEventTopics()

        verify(exactly = 0) { eventTopicService.createOrModifyTopic(any(), any()) }
    }
}
