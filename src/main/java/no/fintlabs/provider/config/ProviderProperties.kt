package no.fintlabs.provider.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fint.provider")
data class ProviderProperties(
    val components: List<ComponentConfig> = emptyList()
)

data class ComponentConfig(
    val domainName: String = "",
    val packageName: String = "",
    val orgIds: List<String> = emptyList(),
    val relationUpdate: Boolean = false
)
