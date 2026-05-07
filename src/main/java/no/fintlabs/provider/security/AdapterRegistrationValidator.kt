package no.fintlabs.provider.security

import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.provider.exception.InvalidAdapterCapabilityException
import no.fintlabs.provider.register.CapabilityEntity
import no.fintlabs.provider.register.ContractJpaRepository
import no.fintlabs.provider.security.resource.ComponentResourceRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AdapterRegistrationValidator(
    private val componentResourceRegistry: ComponentResourceRegistry,
    private val contractRepository: ContractJpaRepository
) {

    companion object {
        const val MAX_FULL_SYNC_INTERVAL_DAYS = 7
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    fun validateContract(contract: AdapterContract) {
        val duplicates = duplicateCapabilityKeysInOrganisation(contract)
        contract.capabilities.forEach { capability ->
            rejectIfDuplicate(capability, contract, duplicates)
            rejectIfUnknownResource(capability)
            rejectIfInvalidFullSyncInterval(capability)
        }
    }

    private fun duplicateCapabilityKeysInOrganisation(contract: AdapterContract): Set<String> =
        contractRepository.findByOrgIdWithCapabilities(contract.orgId)
            .filter { it.userName != contract.username }
            .flatMap { it.capabilityEntityset }
            .mapTo(mutableSetOf()) { it.capabilityKey() }

    private fun rejectIfDuplicate(capability: AdapterCapability, contract: AdapterContract, duplicates: Set<String>) {
        if (capability.capabilityKey() !in duplicates) return
        logger.warn("Rejected registration for '${contract.username}': capability '${capability.entityUri}' is already registered to another adapter in organisation '${contract.orgId}'")
        throw InvalidAdapterCapabilityException("Capability '${capability.entityUri}' is already registered to another adapter in organisation '${contract.orgId}'. Each capability can only be claimed by one adapter per organisation.")
    }

    private fun AdapterCapability.capabilityKey(): String = "$domainName/$packageName/$resourceName"

    private fun CapabilityEntity.capabilityKey(): String = "$domainName/$pkgName/$resourceName"

    private fun rejectIfUnknownResource(capability: AdapterCapability) {
        if (componentResourceRegistry.containsResource(
                capability.domainName,
                capability.packageName,
                capability.resourceName
            )
        ) return
        logger.warn("Rejected registration: capability '${capability.entityUri}' is not a known FINT resource")
        throw InvalidAdapterCapabilityException("Capability '${capability.entityUri}' is not a known FINT resource. Verify the domain, package, and resource names against the metamodel.")
    }

    private fun rejectIfInvalidFullSyncInterval(capability: AdapterCapability) {
        if (capability.fullSyncIntervalInDays in 1..MAX_FULL_SYNC_INTERVAL_DAYS) return
        logger.warn("Rejected registration: capability '${capability.entityUri}' has fullSyncIntervalInDays=${capability.fullSyncIntervalInDays}, must be 1..$MAX_FULL_SYNC_INTERVAL_DAYS")
        throw InvalidAdapterCapabilityException("Capability '${capability.entityUri}' has an invalid fullSyncIntervalInDays value (${capability.fullSyncIntervalInDays}). Must be between 1 and $MAX_FULL_SYNC_INTERVAL_DAYS.")
    }

}