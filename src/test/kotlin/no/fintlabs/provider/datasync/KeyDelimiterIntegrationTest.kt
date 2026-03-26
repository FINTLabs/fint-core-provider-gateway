package no.fintlabs.provider.datasync

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.provider.datasync.EntityProducer.Companion.KEY_DELIMITER
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration
import java.util.*

private const val TOPIC = "fintlabs-no.fint-core.entity.utdanning-vurdering"

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = [TOPIC])
class KeyDelimiterIntegrationTest {

    @Autowired
    private lateinit var entityProducer: EntityProducer

    @Autowired
    private lateinit var broker: EmbeddedKafkaBroker

    @Test
    fun `key with unit separator survives produce and consume`() {
        val expectedResourceName = "elevfravar"
        val expectedIdentifier = UUID.randomUUID().toString()

        val request = RequestFintEvent().apply {
            orgId = "fintlabs.no"
            domainName = "utdanning"
            packageName = "vurdering"
            resourceName = expectedResourceName
        }

        val entry = SyncPageEntry().apply {
            identifier = expectedIdentifier
            resource = mapOf("id" to 42)
        }

        entityProducer.sendEventEntity(request, entry, 0L).get()

        val consumerProps = KafkaTestUtils.consumerProps("test-group", "true", broker)
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        val consumer = DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()
        consumer.subscribe(listOf(TOPIC))

        val records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5))
        val record = records.first()

        val parts = record.key().split(KEY_DELIMITER)
        assertEquals(2, parts.size)
        assertEquals(expectedResourceName, parts[0])
        assertEquals(expectedIdentifier, parts[1])

        consumer.close()
    }
}