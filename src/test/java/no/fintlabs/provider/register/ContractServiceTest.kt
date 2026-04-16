package no.fintlabs.provider.register

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional

class ContractServiceTest {

    private val contractJpaRepository: ContractJpaRepository = mockk(relaxed = true)
    private val contractService = ContractService(contractJpaRepository)

    private val adapterId = "https://test.com/test.org.no/utdanning/elev"
    private val username = "test@adapter.test.org.no"

    @Nested
    inner class SaveContract {

        @Test
        fun `should save contract entity to repository`() {
            val contract = createContract()
            val entitySlot = slot<ContractEntity>()

            every { contractJpaRepository.save(capture(entitySlot)) } answers { entitySlot.captured }

            contractService.saveContract(contract)

            verify { contractJpaRepository.save(any<ContractEntity>()) }
            assertThat(entitySlot.captured.adapterId).isEqualTo(adapterId)
        }
    }

    @Nested
    inner class AdapterCanPerformCapability {

        @Test
        fun `should return true when capability matches`() {
            every { contractJpaRepository.findByUserNameWithCapabilities(adapterId) } returns
                    Optional.of(createContractEntity())

            val result = contractService.adapterCanPerformCapability(adapterId, "utdanning", "elev", "elev")

            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when capability does not match`() {
            every { contractJpaRepository.findByUserNameWithCapabilities(adapterId) } returns
                    Optional.of(createContractEntity())

            val result = contractService.adapterCanPerformCapability(adapterId, "utdanning", "elev", "skoleressurs")

            assertThat(result).isFalse()
        }

        @Test
        fun `should return null when contract not found`() {
            every { contractJpaRepository.findByUserNameWithCapabilities(adapterId) } returns Optional.empty()

            val result = contractService.adapterCanPerformCapability(adapterId, "utdanning", "elev", "elev")

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class UserCanAccessAdapter {

        @Test
        fun `should return true when username matches`() {
            every { contractJpaRepository.findById(username) } returns Optional.of(createContractEntity())

            val result = contractService.userCanAccessAdapter(username, adapterId)

            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when username does not match`() {
            every { contractJpaRepository.findById("wrong@user.no") } returns Optional.of(
                createContractEntity().apply {
                    adapterId = "something-else"
                }
            )

            val result = contractService.userCanAccessAdapter("wrong@user.no", adapterId)

            assertThat(result).isFalse()
        }

        @Test
        fun `should return null when contract not found`() {
            every { contractJpaRepository.findById(username) } returns Optional.empty()

            val result = contractService.userCanAccessAdapter(username, adapterId)

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class GetAdapterIds {

        @Test
        fun `should return adapter ids from repository`() {
            val ids = setOf("adapter-1", "adapter-2")
            every { contractJpaRepository.getAdapterIds() } returns ids

            val result = contractService.getAdapterIds()

            assertThat(result).containsExactlyInAnyOrderElementsOf(ids)
        }
    }

    private fun createContract(): AdapterContract = AdapterContract().apply {
        this.adapterId = this@ContractServiceTest.adapterId
        this.orgId = "test.org.no"
        this.username = this@ContractServiceTest.username
        this.heartbeatIntervalInMinutes = 5
        this.capabilities = setOf(
            AdapterCapability().apply {
                this.domainName = "utdanning"
                this.packageName = "elev"
                this.resourceName = "elev"
                this.fullSyncIntervalInDays = 1
                this.deltaSyncInterval = AdapterCapability.DeltaSyncInterval.IMMEDIATE
            }
        )
    }

    private fun createContractEntity(): ContractEntity = ContractEntity(createContract())
}
