package no.fintlabs.provider.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "fint.provider.kafka")
data class KafkaProperties(
    val entity: EntityKafkaProperties = EntityKafkaProperties(),
    val adapter: AdapterKafkaProperties = AdapterKafkaProperties(),
    val event: EventKafkaProperties = EventKafkaProperties(),
    val relationUpdate: RelationUpdateKafkaProperties = RelationUpdateKafkaProperties()
)

data class EventKafkaProperties(
    val responseProducer: ProducerProperties = ProducerProperties(),
    val responseConsumer: ConsumerProperties = ConsumerProperties(),
    val requestProducer: ProducerProperties = ProducerProperties(),
    val requestConsumer: ConsumerProperties = ConsumerProperties()
)

data class ProducerProperties(
    val partitions: Int = 1,
    val retentionTime: Duration = Duration.ofDays(7)
)

data class ConsumerProperties(
    val concurrency: Int = 1
)

data class EntityKafkaProperties(
    val partitions: Int = 6,
    val retentionTime: Duration = Duration.ofDays(30)
)

data class RelationUpdateKafkaProperties(
    val partitions: Int = 6,
    val retentionTime: Duration = Duration.ofDays(30)
)

data class AdapterKafkaProperties(
    val partitions: Int = 1,
    val heartbeatRetentionTime: Duration = Duration.ofDays(1),
    val registerRetentionTime: Duration = Duration.ofMillis(Long.MAX_VALUE),
    val fullSyncRetentionTime: Duration = Duration.ofDays(1),
    val deltaSyncRetentionTime: Duration = Duration.ofDays(1),
    val deleteSyncRetentionTime: Duration = Duration.ofDays(1)
)
