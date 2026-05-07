package no.fintlabs.provider.security

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.provider.exception.InvalidAdapterCapabilityException
import no.fintlabs.provider.register.CapabilityEntity
import no.fintlabs.provider.register.ContractEntity
import no.fintlabs.provider.register.ContractJpaRepository
import no.fintlabs.provider.security.resource.ComponentResourceRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AdapterRegistrationValidatorTest {

    @MockK
    lateinit var componentResourceRegistry: ComponentResourceRegistry

    @MockK
    lateinit var contractRepository: ContractJpaRepository

    @InjectMockKs
    lateinit var sut: AdapterRegistrationValidator

    private val orgId = "test.org.no"
    private val username = "user@adapter.test.org.no"

    @Test
    fun `valid contract with no existing contracts in organisation is accepted`() {
        every { contractRepository.findByOrgIdWithCapabilities(orgId) } returns emptyList()
        every { componentResourceRegistry.containsResource("utdanning", "elev", "elev") } returns true

        assertDoesNotThrow {
            sut.validateContract(contract(capabilities = setOf(capability("utdanning", "elev", "elev"))))
        }
    }

    @Test
    fun `duplicate triple from another user in same organisation is rejected`() {
        every { contractRepository.findByOrgIdWithCapabilities(orgId) } returns
            listOf(existingContract("other@adapter.test.org.no", capability("utdanning", "elev", "elev")))

        assertThrows<InvalidAdapterCapabilityException> {
            sut.validateContract(contract(capabilities = setOf(capability("utdanning", "elev", "elev"))))
        }
    }

    @Test
    fun `same resource name under a different package is not a duplicate`() {
        every { contractRepository.findByOrgIdWithCapabilities(orgId) } returns
            listOf(existingContract("other@adapter.test.org.no", capability("administrasjon", "personal", "fravar")))
        every { componentResourceRegistry.containsResource("utdanning", "vurdering", "fravar") } returns true

        assertDoesNotThrow {
            sut.validateContract(contract(capabilities = setOf(capability("utdanning", "vurdering", "fravar"))))
        }
    }

    @Test
    fun `same resource name under a different domain is not a duplicate`() {
        every { contractRepository.findByOrgIdWithCapabilities(orgId) } returns
            listOf(existingContract("other@adapter.test.org.no", capability("utdanning", "elev", "kontaktlarergruppe")))
        every { componentResourceRegistry.containsResource("administrasjon", "elev", "kontaktlarergruppe") } returns true

        assertDoesNotThrow {
            sut.validateContract(
                contract(capabilities = setOf(capability("administrasjon", "elev", "kontaktlarergruppe")))
            )
        }
    }

    @Test
    fun `same user re-registering an existing capability is accepted`() {
        every { contractRepository.findByOrgIdWithCapabilities(orgId) } returns
            listOf(existingContract(username, capability("utdanning", "elev", "elev")))
        every { componentResourceRegistry.containsResource("utdanning", "elev", "elev") } returns true

        assertDoesNotThrow {
            sut.validateContract(contract(capabilities = setOf(capability("utdanning", "elev", "elev"))))
        }
    }

    private fun contract(capabilities: Set<AdapterCapability>) =
        AdapterContract().apply {
            this.orgId = this@AdapterRegistrationValidatorTest.orgId
            this.username = this@AdapterRegistrationValidatorTest.username
            this.capabilities = capabilities
        }

    private fun capability(domain: String, pkg: String, resource: String, fullSyncIntervalInDays: Int = 1) =
        AdapterCapability().apply {
            this.domainName = domain
            this.packageName = pkg
            this.resourceName = resource
            this.fullSyncIntervalInDays = fullSyncIntervalInDays
            this.deltaSyncInterval = AdapterCapability.DeltaSyncInterval.IMMEDIATE
        }

    private fun existingContract(userName: String, vararg capabilities: AdapterCapability): ContractEntity {
        val entity = ContractEntity()
        entity.userName = userName
        entity.capabilityEntityset = capabilities.map { capability ->
            CapabilityEntity(capability).also { it.contractEntity = entity }
        }.toSet()
        return entity
    }
}
