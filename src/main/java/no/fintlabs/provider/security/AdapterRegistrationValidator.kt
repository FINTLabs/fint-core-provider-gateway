package no.fintlabs.provider.security

import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.provider.exception.InvalidAdapterCapabilityException
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
        val duplicates = duplicateResourceNamesInOrganisation(contract)
        contract.capabilities.forEach { capability ->
            rejectIfDuplicate(capability, contract.orgId, duplicates)
            rejectIfUnknownResource(capability)
            rejectIfInvalidFullSyncInterval(capability)
        }
    }

    private fun duplicateResourceNamesInOrganisation(contract: AdapterContract): Set<String> =
        contractRepository.findByOrgIdWithCapabilities(contract.orgId)
            .filter { it.userName != contract.username }
            .flatMap { it.capabilityEntityset }
            .mapTo(mutableSetOf()) { it.resourceName }

    private fun rejectIfDuplicate(capability: AdapterCapability, orgId: String, duplicates: Set<String>) {
        if (capability.resourceName !in duplicates) return
        logger.warn("Validation failed: Capability '$capability' from '${capability.entityUri}' is a duplicate in organisation '$orgId'")
        throw InvalidAdapterCapabilityException("Duplicate capability resource: ${capability.entityUri} - Organisation already has a capability with the same resource name")
    }

    private fun rejectIfUnknownResource(capability: AdapterCapability) {
        if (componentResourceRegistry.containsResource(
                capability.domainName,
                capability.packageName,
                capability.resourceName
            )
        ) return
        logger.warn("Validation failed: Capability '$capability' from '${capability.entityUri}' is not a valid resource.")
        throw InvalidAdapterCapabilityException("Invalid capability resource: ${capability.entityUri} - Component does not exist")
    }

    private fun rejectIfInvalidFullSyncInterval(capability: AdapterCapability) {
        if (capability.fullSyncIntervalInDays in 1..MAX_FULL_SYNC_INTERVAL_DAYS) return
        logger.warn("Validation failed: Capability '$capability' has an invalid FullSyncIntervalInDays value")
        throw InvalidAdapterCapabilityException("Invalid capability resource: ${capability.entityUri} - FullSyncIntervalInDays value is invalid")
    }

}