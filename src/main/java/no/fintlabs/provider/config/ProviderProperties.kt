package no.fintlabs.provider.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "fint.provider")
data class ProviderProperties(
    val kafka: KafkaConfiguration = KafkaConfiguration()
)

data class KafkaConfiguration(
    val entityPartitions: Int = 1,
    val adapter: AdapterKafkaConfiguration = AdapterKafkaConfiguration()
)

data class AdapterKafkaConfiguration(
    val heartbeatRetentionTime: Duration = Duration.ofDays(1),
    val registerRetentionTime: Duration = Duration.ofMillis(Long.MAX_VALUE),
    val fullSyncRetentionTime: Duration = Duration.ofDays(1),
    val deltaSyncRetentionTime: Duration = Duration.ofDays(1),
    val deleteSyncRetentionTime: Duration = Duration.ofDays(1),
)