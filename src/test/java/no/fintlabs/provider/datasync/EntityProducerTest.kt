package no.fintlabs.provider.datasync

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Clock
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.adapter.models.sync.SyncType
import no.fintlabs.provider.kafka.ProviderTopicService
import no.fintlabs.provider.kafka.TopicNamesConstants.LAST_MODIEFIED
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_CORRELATION_ID
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_TOTAL_SIZE
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_TYPE
import no.fintlabs.provider.kafka.TopicNamesConstants.TOPIC_RETENTION_TIME
import no.novari.kafka.producing.ParameterizedProducerRecord
import no.novari.kafka.producing.ParameterizedTemplate
import no.novari.kafka.producing.ParameterizedTemplateFactory
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.SendResult

class EntityProducerTest {

    private lateinit var factory: ParameterizedTemplateFactory
    private lateinit var topicService: ProviderTopicService
    private lateinit var kafkaProducer: ParameterizedTemplate<Any>
    private lateinit var clock: Clock
    private lateinit var sut: EntityProducer

    @BeforeEach
    fun setup() {
        factory = mockk()
        topicService = mockk()
        kafkaProducer = mockk()
        clock = mockk()

        every { factory.createTemplate(Any::class.java) } returns kafkaProducer
        every { kafkaProducer.send(any<ParameterizedProducerRecord<Any>>()) } answers {
            CompletableFuture.completedFuture(mockk<SendResult<String, Any>>(relaxed = true))
        }

        sut = EntityProducer(factory, topicService, clock)
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `sendSyncEntity builds expected topic and headers`() {
        val expectedLastModified = 1337L
        val expectedTopicRetention = Duration.ofMillis(42)
        val expectedSyncType = SyncType.FULL
        val expectedSyncCorrId = UUID.randomUUID().toString()
        val expectedSyncTotalSize = 9L

        val syncPage = SyncPage(expectedSyncType).apply {
            metadata = SyncPageMetadata().apply {
                orgId = "fintlabs.no"
                corrId = expectedSyncCorrId
                uriRef = "utdanning/elev/student"
                totalSize = expectedSyncTotalSize
            }
        }
        val entry = SyncPageEntry().apply {
            identifier = UUID.randomUUID().toString()
            resource = mapOf("id" to 42)
        }

        every { topicService.getRetentionTime(any<TopicNameParameters>()) } returns expectedTopicRetention
        every { clock.millis() } returns expectedLastModified

        val record = sendAndCapture { sut.sendSyncEntity(syncPage, entry) }

        val expected = EntityTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters
                    .stepBuilder()
                    .orgId("fintlabs-no")
                    .domainContextApplicationDefault()
                    .build()
            )
            .resourceName("utdanning-elev-student")
            .build()

        assertEquals(expected, record.topicNameParameters)

        assertEquals(expectedLastModified, record.getHeaderValue(LAST_MODIEFIED).long())
        assertEquals(expectedTopicRetention.toMillis(), record.getHeaderValue(TOPIC_RETENTION_TIME).long())
        assertEquals(expectedSyncType.ordinal.toByte(), record.getHeaderValue(SYNC_TYPE).first())
        assertEquals(expectedSyncCorrId, record.getHeaderValue(SYNC_CORRELATION_ID).toString(Charset.defaultCharset()))
        assertEquals(expectedSyncTotalSize, record.getHeaderValue(SYNC_TOTAL_SIZE).long())

        assertEquals(entry.identifier, record.key)
        assertEquals(entry.resource, record.value)
    }

    @Test
    fun `sendEventEntity builds expected topic and headers`() {
        val expectedLastModified = 133710428L
        val expectedTopicRetention = Duration.ofMillis(3489138423L)
        val request = RequestFintEvent().apply {
            orgId = "fintlabs.no"
            domainName = "utdanning"
            packageName = "vurdering"
            resourceName = "elevfravar"
        }
        val entry = SyncPageEntry().apply {
            identifier = UUID.randomUUID().toString()
            resource = mapOf("id" to 42)
        }

        every { topicService.getRetentionTime(any<TopicNameParameters>()) } returns expectedTopicRetention
        every { clock.millis() } returns expectedLastModified

        val record = sendAndCapture { sut.sendEventEntity(request, entry) }

        val expected = EntityTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters
                    .stepBuilder()
                    .orgId("fintlabs-no")
                    .domainContextApplicationDefault()
                    .build()
            )
            .resourceName("utdanning-vurdering-elevfravar")
            .build()

        assertEquals(expected, record.topicNameParameters)
        assertEquals(expectedLastModified, record.getHeaderValue(LAST_MODIEFIED).long())
        assertEquals(expectedTopicRetention.toMillis(), record.getHeaderValue(TOPIC_RETENTION_TIME).long())
        assertNull(record.getHeader(SYNC_TYPE))
        assertNull(record.getHeader(SYNC_CORRELATION_ID))
        assertNull(record.getHeader(SYNC_TOTAL_SIZE))

        assertEquals(entry.identifier, record.key)
        assertEquals(entry.resource, record.value)
    }

    private fun ByteArray.long(): Long = ByteBuffer.wrap(this).long
    private fun ParameterizedProducerRecord<Any>.getHeaderValue(key: String) = this.headers.lastHeader(key).value()
    private fun ParameterizedProducerRecord<Any>.getHeader(key: String) = this.headers.lastHeader(key)

    private fun sendAndCapture(block: () -> Unit): ParameterizedProducerRecord<Any> {
        val slot = slot<ParameterizedProducerRecord<Any>>()
        every { kafkaProducer.send(capture(slot)) } answers {
            CompletableFuture.completedFuture(mockk(relaxed = true))
        }
        block()
        return slot.captured
    }

}
