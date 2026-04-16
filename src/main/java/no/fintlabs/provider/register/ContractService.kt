package no.fintlabs.provider.register

import no.fintlabs.adapter.models.AdapterContract
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ContractService(
    private val contractJpaRepository: ContractJpaRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAdapterIds(): Set<String> = contractJpaRepository.getAdapterIds()

    fun saveContract(adapterContract: AdapterContract) {
        contractJpaRepository.save(ContractEntity(adapterContract))
        log.info("AdapterContract saved: {}", adapterContract.username)
    }

    /**
     * @return `true` if capability matches, `false` if not, `null` if contract not found.
     */
    fun adapterCanPerformCapability(
        username: String,
        domainName: String,
        packageName: String,
        entityName: String
    ): Boolean? =
        contractJpaRepository.findByUserNameWithCapabilities(username)
            .map { contract -> contract.capabilityEntityset.any { it.matches(domainName, packageName, entityName) } }
            .orElse(null)

    /**
     * @return `true` if username matches, `false` if not, `null` if contract not found.
     */
    fun userCanAccessAdapter(username: String): Boolean? =
        contractJpaRepository.findById(username)
            .map { it.userName == username }
            .orElse(null)

    private fun CapabilityEntity.matches(domainName: String, packageName: String, resourceName: String): Boolean =
        this.domainName == domainName && this.pkgName == packageName && this.resourceName == resourceName
}
