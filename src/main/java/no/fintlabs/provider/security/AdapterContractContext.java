package no.fintlabs.provider.security;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
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
        return contractJpaRepository.findByAdapterIdWithCapabilities(adapterId).get()
                .getCapabilityEntityset().stream()
                .anyMatch(capabilityEntity ->
                        capabilityEntity.getDomainName().equals(domainName) &&
                                capabilityEntity.getPkgName().equals(packageName) &&
                                capabilityEntity.getResourceName().equals(entityName));
    }

    public void saveOrUpdateContract(AdapterContract adapterContract) {
        if (contractJpaRepository.existsById(adapterContract.getAdapterId())) {
            contractJpaRepository.save(new ContractEntity(adapterContract));
            log.info("AdapterContract updated: {}", adapterContract.getAdapterId());
        } else {
            adapterContract.setTime(System.currentTimeMillis());
            contractJpaRepository.save(new ContractEntity(adapterContract));
            log.info("New AdapterContract registered: {}", adapterContract.getAdapterId());

        }
    }

    public boolean userCanAccessAdapter(String username, String adapterId) {
        return contractJpaRepository.getReferenceById(adapterId).getUsername().equals(username);
    }
}
