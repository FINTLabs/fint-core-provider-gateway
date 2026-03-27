package no.fintlabs.provider.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fint.provider")
data class ProviderProperties(
    val orgIds: List<String> = emptyList()
)
