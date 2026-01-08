package no.fintlabs.provider.gateway.security

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.provider.exception.InvalidAdapterCapabilityException
import no.fintlabs.provider.security.AdapterRegistrationValidator
import no.fintlabs.provider.security.resource.ComponentResourceRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AdapterRegistrationValidatorTest {

    @MockK
    lateinit var componentResourceRegistry: ComponentResourceRegistry

    @InjectMockKs
    lateinit var sut: AdapterRegistrationValidator

    private val domainName = "utdanning"
    private val packageName = "vurdering"
    private val resourceName = "elevfravar"

    @Test
    fun `empty list of capabilities are valid`() {
        assertDoesNotThrow { sut.validateCapabilities(listOf()) }
    }

    @Test
    fun `existing component-resource in registry is valid`() {
        val capability = createCapability(domainName, packageName, resourceName)

        every { componentResourceRegistry.containsResource(domainName, packageName, resourceName) } returns true

        assertDoesNotThrow { sut.validateCapabilities(listOf(capability)) }
    }

    @Test
    fun `non-existing component-resource in registry is invalid`() {
        val capability = createCapability(domainName, packageName, resourceName)

        every { componentResourceRegistry.containsResource(domainName, packageName, resourceName) } returns false

        assertThrows<InvalidAdapterCapabilityException> { sut.validateCapabilities(listOf(capability)) }
    }

    @Test
    fun `fullSyncIntervalInDays between 1 and 7 is valid`() {
        val capabilities = (1..7).map { createCapability(fullSyncIntervalInDays = it) }

        every { componentResourceRegistry.containsResource(domainName, packageName, resourceName) } returns true

        assertDoesNotThrow { sut.validateCapabilities(capabilities) }
    }

    @Test
    fun `fullSyncIntervalInDays under 1 is invalid`() {
        val capability = createCapability(fullSyncIntervalInDays = 0)

        every { componentResourceRegistry.containsResource(domainName, packageName, resourceName) } returns true

        assertThrows<InvalidAdapterCapabilityException> { sut.validateCapabilities(listOf(capability)) }
    }

    @Test
    fun `fullSyncIntervalInDays above 7 days is invalid`() {
        val capability = createCapability(fullSyncIntervalInDays = 8)

        every { componentResourceRegistry.containsResource(domainName, packageName, resourceName) } returns true

        assertThrows<InvalidAdapterCapabilityException> { sut.validateCapabilities(listOf(capability)) }
    }

    private fun createCapability(
        domainName: String = this.domainName,
        packageName: String = this.packageName,
        resourceName: String = this.resourceName,
        fullSyncIntervalInDays: Int = 1,
    ) =
        AdapterCapability().apply {
            this.domainName = domainName
            this.packageName = packageName
            this.resourceName = resourceName
            this.fullSyncIntervalInDays = fullSyncIntervalInDays
        }


}