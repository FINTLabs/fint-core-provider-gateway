package no.fintlabs.provider.security;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.provider.exception.AdapterNotRegisteredException;
import no.fintlabs.provider.register.ContractEntity;
import no.fintlabs.provider.register.ContractJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class AdapterContractContext {

    private ContractJpaRepository contractJpaRepository;

    public AdapterContractContext(ContractJpaRepository contractJpaRepository) {
        this.contractJpaRepository = contractJpaRepository;
    }

    public Set<String> getAdapterIds() {
        return contractJpaRepository.getAdapterIds();
    }

    public boolean adapterCanPerformCapability(String adapterId, String domainName, String packageName, String entityName) {
        ContractEntity contract = contractJpaRepository.findByAdapterIdWithCapabilities(adapterId)
                .orElseThrow(() -> {
                    log.error("Adapter is not registered: {}", adapterId);
                    return new AdapterNotRegisteredException("Cant perform action; because adapter is not yet registered");
                });

        boolean canPerform = contract.getCapabilityEntityset().stream()
                .anyMatch(capabilityEntity ->
                        capabilityEntity.getDomainName().equals(domainName) &&
                                capabilityEntity.getPkgName().equals(packageName) &&
                                capabilityEntity.getResourceName().equals(entityName));

        if (canPerform) {
            return true;
        }

        log.error("Adapter {} cannot perform capability {}/{}/{}", adapterId, domainName, packageName, entityName);
        throw new AdapterNotRegisteredException("Cant perform action; because adapter is not yet registered");
    }

    public void add(AdapterContract adapterContract) {
        if (validateCapabilities(adapterContract)) {
            contractJpaRepository.save(new ContractEntity(adapterContract));
        } else {
            log.error("Invalid Capability");
        }
    }

    public boolean userCanAccessAdapter(String username, String adapterId) {
        return contractJpaRepository.getReferenceById(adapterId).getUsername().equals(username);
    }

    private boolean validateCapabilities(AdapterContract adapterContract) {
        boolean allValid = adapterContract.getCapabilities().stream()
                .allMatch(capability ->
                        validateFullsyncInterval(capability.getFullSyncIntervalInDays()) &&
                                validateDomainPackageAndResourcesIsSet(adapterContract)
                );

        if (!allValid) {
            throw new IllegalArgumentException("Invalid Capability");
        }

        return true;
    }

    private boolean validateFullsyncInterval(int fullSyncIntervalInDays) {
        if (fullSyncIntervalInDays >= 1 && fullSyncIntervalInDays <= 7) {
            return true;
        }
        throw new IllegalArgumentException("FullSyncIntervalInDays must be between 1 and 7");
    }

    private boolean validateDomainPackageAndResourcesIsSet(AdapterContract adapterContract) {
        return adapterContract.getCapabilities().stream().allMatch(capability -> capability.getDomainName() != null && capability.getPackageName() != null && capability.getResourceName() != null);
    }
}
