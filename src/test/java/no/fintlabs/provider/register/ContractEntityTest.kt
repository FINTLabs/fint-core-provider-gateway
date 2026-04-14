package no.fintlabs.provider.register

import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ContractEntityTest {

    @Nested
    inner class MappingFromAdapterContract {

        @Test
        fun `should map contract fields correctly`() {
            val contract = createContract()

            val entity = ContractEntity(contract)

            assertThat(entity.adapterId).isEqualTo("adapter-1")
            assertThat(entity.orgId).isEqualTo("test.org.no")
            assertThat(entity.username).isEqualTo("test@adapter.test.org.no")
            assertThat(entity.heartbeatIntervalInMinutes).isEqualTo(5)
        }

        @Test
        fun `should map capabilities correctly`() {
            val contract = createContract(
                capabilities = setOf(
                    createCapability("utdanning", "elev", "elev"),
                    createCapability("utdanning", "elev", "skoleressurs")
                )
            )

            val entity = ContractEntity(contract)

            assertThat(entity.capabilityEntityset).hasSize(2)
            assertThat(entity.capabilityEntityset.map { it.resourceName })
                .containsExactlyInAnyOrder("elev", "skoleressurs")
        }

        @Test
        fun `should set back-reference on capabilities`() {
            val contract = createContract()

            val entity = ContractEntity(contract)

            entity.capabilityEntityset.forEach {
                assertThat(it.contractEntity).isSameAs(entity)
            }
        }

        @Test
        fun `should map capability fields correctly`() {
            val contract = createContract(
                capabilities = setOf(createCapability("utdanning", "elev", "elev"))
            )

            val entity = ContractEntity(contract)
            val capability = entity.capabilityEntityset.first()

            assertThat(capability.domainName).isEqualTo("utdanning")
            assertThat(capability.pkgName).isEqualTo("elev")
            assertThat(capability.resourceName).isEqualTo("elev")
            assertThat(capability.fullSyncIntervalInDays).isEqualTo(1)
            assertThat(capability.deltaSyncInterval).isEqualTo("IMMEDIATE")
        }

        @Test
        fun `should handle empty capabilities`() {
            val contract = createContract(capabilities = emptySet())

            val entity = ContractEntity(contract)

            assertThat(entity.capabilityEntityset).isEmpty()
        }
    }

    @Nested
    inner class UpdatingContract {

        @Test
        fun `should produce new capability set when contract is recreated with different capabilities`() {
            val original = createContract(
                capabilities = setOf(createCapability("utdanning", "elev", "elev"))
            )
            val updated = createContract(
                capabilities = setOf(
                    createCapability("utdanning", "elev", "skoleressurs"),
                    createCapability("administrasjon", "personal", "person")
                )
            )

            val originalEntity = ContractEntity(original)
            val updatedEntity = ContractEntity(updated)

            assertThat(originalEntity.capabilityEntityset).hasSize(1)
            assertThat(updatedEntity.capabilityEntityset).hasSize(2)
            assertThat(updatedEntity.capabilityEntityset.map { it.resourceName })
                .containsExactlyInAnyOrder("skoleressurs", "person")
        }

        @Test
        fun `should reflect updated fields when contract is recreated`() {
            val original = createContract(username = "old@adapter.test.org.no")
            val updated = createContract(username = "new@adapter.test.org.no")

            val originalEntity = ContractEntity(original)
            val updatedEntity = ContractEntity(updated)

            assertThat(originalEntity.username).isEqualTo("old@adapter.test.org.no")
            assertThat(updatedEntity.username).isEqualTo("new@adapter.test.org.no")
            assertThat(updatedEntity.adapterId).isEqualTo(originalEntity.adapterId)
        }
    }

    private fun createContract(
        adapterId: String = "adapter-1",
        username: String = "test@adapter.test.org.no",
        capabilities: Set<AdapterCapability> = setOf(createCapability("utdanning", "elev", "elev"))
    ): AdapterContract = AdapterContract().apply {
        this.adapterId = adapterId
        this.orgId = "test.org.no"
        this.username = username
        this.heartbeatIntervalInMinutes = 5
        this.capabilities = capabilities
    }

    private fun createCapability(domain: String, pkg: String, resource: String): AdapterCapability =
        AdapterCapability().apply {
            this.domainName = domain
            this.packageName = pkg
            this.resourceName = resource
            this.fullSyncIntervalInDays = 1
            this.deltaSyncInterval = AdapterCapability.DeltaSyncInterval.IMMEDIATE
        }
}
