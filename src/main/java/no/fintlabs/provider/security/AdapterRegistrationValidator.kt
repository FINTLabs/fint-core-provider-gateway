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

    fun validateContract(contract: AdapterContract) =
        contract.capabilities.forEach { capability ->
            if (isDuplicateInOrganisation(capability, contract.orgId, contract.username)) {
                logger.warn("Validation failed: Capability '$capability' from '${capability.entityUri}' is a duplicate in organisation '${contract.orgId}'")
                throw InvalidAdapterCapabilityException("Duplicate capability resource: ${capability.entityUri} - Organisation already has a capability with the same resource name")
            }
            if (invalidComponentResource(capability)) {
                logger.warn("Validation failed: Capability '$capability' from '${capability.entityUri}' is not a valid resource.")
                throw InvalidAdapterCapabilityException("Invalid capability resource: ${capability.entityUri} - Component does not exist")
            } else if (invalidFullSyncInterval(capability.fullSyncIntervalInDays)) {
                logger.warn("Validation failed: Capability '$capability' has an invalid FullSyncIntervalInDays value")
                throw InvalidAdapterCapabilityException("Invalid capability resource: ${capability.entityUri} - FullSyncIntervalInDays value is invalid")
            }
        }

    private fun isDuplicateInOrganisation(
        capability: AdapterCapability,
        orgId: String,
        userName: String
    ): Boolean {
        return contractRepository.getCapabilitiesOnOrId(orgId)
            .any { contract ->
                contract.userName != userName &&
                        contract.capabilityEntityset.any {
                            it.resourceName == capability.resourceName
                        }
            }
    }

    private fun invalidComponentResource(capability: AdapterCapability): Boolean =
        !componentResourceRegistry.containsResource(
            capability.domainName,
            capability.packageName,
            capability.resourceName
        )

    private fun invalidFullSyncInterval(fullSyncIntervalInDays: Int): Boolean =
        fullSyncIntervalInDays !in 1..MAX_FULL_SYNC_INTERVAL_DAYS

}