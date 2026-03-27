package no.fintlabs.provider.kafka

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.provider.config.AdapterKafkaProperties
import no.novari.kafka.topic.EventTopicService
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EventTopicEnsurerTest {

    private lateinit var eventTopicService: EventTopicService
    private val adapterKafkaProperties = AdapterKafkaProperties()
    private lateinit var sut: EventTopicEnsurer

    @BeforeEach
    fun setup() {
        eventTopicService = mockk()
        every { eventTopicService.createOrModifyTopic(any(), any()) } just Runs
        sut = EventTopicEnsurer(adapterKafkaProperties, eventTopicService)
    }

    @Test
    fun `ensureEventTopics creates all five adapter event topics`() {
        sut.ensureEventTopics()

        verify(exactly = 5) { eventTopicService.createOrModifyTopic(any(), any()) }
    }

    @Test
    fun `ensureEventTopics creates topic for each expected event name`() {
        sut.ensureEventTopics()

        listOf(
            TopicNamesConstants.HEARTBEAT_EVENT_NAME,
            TopicNamesConstants.ADAPTER_REGISTER_EVENT_NAME,
            TopicNamesConstants.ADAPTER_FULL_SYNC_EVENT_NAME,
            TopicNamesConstants.ADAPTER_DELTA_SYNC_EVENT_NAME,
            TopicNamesConstants.ADAPTER_DELETE_SYNC_EVENT_NAME
        ).forEach { eventName ->
            val expected = EventTopicNameParameters.builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters.stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build()
                )
                .eventName(eventName)
                .build()

            verify(exactly = 1) { eventTopicService.createOrModifyTopic(expected, any()) }
        }
    }
}
