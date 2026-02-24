package no.fintlabs.provider.datasync

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.adapter.models.sync.SyncType
import no.fintlabs.kafka.common.topic.TopicNameParameters
import no.fintlabs.kafka.entity.EntityProducerFactory
import no.fintlabs.kafka.entity.EntityProducerRecord
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import no.fintlabs.provider.kafka.TopicNamesConstants.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.SendResult
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Clock
import java.util.*
import java.util.concurrent.CompletableFuture

class EntityProducerTest {

    private lateinit var factory: EntityProducerFactory
    private lateinit var topicService: ProviderTopicService
    private lateinit var kafkaProducer: no.fintlabs.kafka.entity.EntityProducer<Any>
    private lateinit var clock: Clock
    private lateinit var sut: EntityProducer

    @BeforeEach
    fun setup() {
        factory = mockk()
        topicService = mockk()
        kafkaProducer = mockk()
        clock = mockk()

        every { factory.createProducer(Any::class.java) } returns kafkaProducer
        every { kafkaProducer.send(any<EntityProducerRecord<Any>>()) } answers {
            CompletableFuture.completedFuture(mockk<SendResult<String, Any>>(relaxed = true))
        }

        sut = EntityProducer(factory, topicService, clock)
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `sendSyncEntity builds expected topic and headers`() {
        val expectedLastModified = 1337L
        val expectedTopicRetention = 42L
        val expectedSynctype = SyncType.FULL
        val expectedSyncCorrId = UUID.randomUUID().toString()
        val expectedSyncTotalSize = 9L

        val syncPage = SyncPage(expectedSynctype).apply {
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

        val expectedTopic = EntityTopicNameParameters.builder()
            .orgId("fintlabs-no")
            .domainContext(FINT_CORE)
            .resource("utdanning-elev-student")
            .build()

        every { topicService.getRetensionTime(any<TopicNameParameters>()) } returns expectedTopicRetention
        every { clock.millis() } returns expectedLastModified

        val record = sendAndCapture { sut.sendSyncEntity(syncPage, entry) }

        assertEquals(expectedTopic, record.topicNameParameters)

        // Default-Header values match
        assertEquals(expectedLastModified, record.getHeaderValue(LAST_UPDATED).long())
        assertEquals(expectedTopicRetention, record.getHeaderValue(TOPIC_RETENTION_TIME).long())

        // Sync-Header values match
        assertEquals(expectedSynctype.ordinal.toByte(), record.getHeaderValue(SYNC_TYPE).first())
        assertEquals(expectedSyncCorrId, record.getHeaderValue(SYNC_CORRELATION_ID).toString(Charset.defaultCharset()))
        assertEquals(expectedSyncTotalSize, record.getHeaderValue(SYNC_TOTAL_SIZE).long())

        assertEquals(entry.identifier, record.key)
        assertEquals(entry.resource, record.value)
    }

    @Test
    fun `sendEventEntity builds expected topic and headers`() {
        val expectedLastModified = 133710428L
        val expectedTopicRetention = 3489138423L
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

        val expectedTopic = EntityTopicNameParameters.builder()
            .orgId("fintlabs-no")
            .domainContext(FINT_CORE)
            .resource("utdanning-vurdering-elevfravar")
            .build()


        every { topicService.getRetensionTime(any<TopicNameParameters>()) } returns expectedTopicRetention
        every { clock.millis() } returns expectedLastModified

        val record = sendAndCapture { sut.sendEventEntity(request, entry, expectedLastModified) }

        // Topic matches
        assertEquals(expectedTopic, record.topicNameParameters)

        // Default-Header values match
        assertEquals(expectedLastModified, record.getHeaderValue(LAST_UPDATED).long())
        assertEquals(expectedTopicRetention, record.getHeaderValue(TOPIC_RETENTION_TIME).long())

        // Headers are not set
        assertNull(record.getHeader(SYNC_TYPE))
        assertNull(record.getHeader(SYNC_CORRELATION_ID))
        assertNull(record.getHeader(SYNC_TOTAL_SIZE))

        assertEquals(entry.identifier, record.key)
        assertEquals(entry.resource, record.value)
    }

    private fun ByteArray.long(): Long = ByteBuffer.wrap(this).long
    private fun EntityProducerRecord<Any>.getHeaderValue(key: String) = this.headers.lastHeader(key).value()
    private fun EntityProducerRecord<Any>.getHeader(key: String) = this.headers.lastHeader(key)

    private fun sendAndCapture(block: () -> Unit): EntityProducerRecord<Any> {
        val slot = slot<EntityProducerRecord<Any>>()
        every { kafkaProducer.send(capture(slot)) } answers {
            CompletableFuture.completedFuture(mockk(relaxed = true))
        }
        block()
        return slot.captured
    }

}