package no.fintlabs.provider.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "fint.provider.cleanup-topics")
data class CleanupTopicsProperties(
    val enabled: Boolean = false,
    val batchSize: Int = 10,
    val batchDelay: Duration = Duration.ofSeconds(30),
)
