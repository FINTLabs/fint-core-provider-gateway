package no.fintlabs.provider.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.provider.exception.InvalidAdapterCapabilityException;
import no.fintlabs.provider.security.resource.ResourceContext;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterRegistrationValidator {

    private final ResourceContext resourceContext;

    public void validateCapabilities(Set<AdapterCapability> capabilities) {
        capabilities.forEach(capability -> {
            String componentResource = "%s-%s-%s".formatted(capability.getDomainName(), capability.getPackageName(), capability.getResourceName()).toLowerCase();
            if (!resourceContext.getValidResources().contains(componentResource)) {
                log.warn("Validation failed: Capability '{}' from '{}' is not a valid resource.", capability, componentResource);
                throw new InvalidAdapterCapabilityException("Invalid capability resource: %s - Component does not exist".formatted(componentResource));
            } else if (invalidFullSyncInterval(capability.getFullSyncIntervalInDays())) {
                log.warn("Validation failed: Capability '{}' has an invalid FullSyncIntervalInDays value", capability.getEntityUri());
                throw new InvalidAdapterCapabilityException("Invalid capability resource: %s - FullSyncIntervalInDays value is invalid".formatted(componentResource));
            }
        });
    }

    private boolean invalidFullSyncInterval(int fullSyncIntervalInDays) {
        return fullSyncIntervalInDays <= 0;
    }

}
