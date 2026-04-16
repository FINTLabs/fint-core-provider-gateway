package no.fintlabs.provider.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource

class ProviderPropertiesTest {

    private fun loadFromYaml(resource: String): ProviderProperties {
        val sources = YamlPropertySourceLoader().load("test", ClassPathResource(resource))
        val environment = org.springframework.core.env.StandardEnvironment()
        sources.forEach { environment.propertySources.addFirst(it) }
        return Binder.get(environment).bind("fint.provider", ProviderProperties::class.java).get()
    }

    @Test
    fun `components are loaded with correct domain-name and package-name`() {
        val props = loadFromYaml("provider-properties-test.yaml")

        assertEquals(4, props.components.size)
        assertEquals("utdanning", props.components[0].domainName)
        assertEquals("vurdering", props.components[0].packageName)
        assertEquals("utdanning", props.components[1].domainName)
        assertEquals("elev", props.components[1].packageName)
    }

    @Test
    fun `yaml anchors resolve org-ids identically across components`() {
        val props = loadFromYaml("provider-properties-test.yaml")

        val expectedOrgs = listOf("fintlabs-no", "rogfk-no", "afk-no")
        props.components.forEach { component ->
            assertEquals(expectedOrgs, component.orgIds,
                "${component.domainName}-${component.packageName} should have matching org-ids")
        }
    }

    @Test
    fun `relation-update flag is loaded correctly`() {
        val props = loadFromYaml("provider-properties-test.yaml")

        assertTrue(props.components[0].relationUpdate, "vurdering should have relationUpdate=true")
        assertTrue(props.components[1].relationUpdate, "elev should have relationUpdate=true")
        assertFalse(props.components[2].relationUpdate, "ot should have relationUpdate=false")
        assertFalse(props.components[3].relationUpdate, "personal should have relationUpdate=false")
    }
}
