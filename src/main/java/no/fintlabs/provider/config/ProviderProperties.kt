package no.fintlabs.provider.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fint.provider")
data class ProviderProperties(
    val adapterHeartbeatRetentionTimeMs: Long = 86400000,
    val adapterRegisterRetentionTimeMs: Long = Long.MAX_VALUE,
    val adapterFullSyncRetentionTimeMs: Long = 86400000,
    val adapterDeltaSyncRetentionTimeMs: Long = 86400000,
    val adapterDeleteSyncRetentionTimeMs: Long = 86400000
)