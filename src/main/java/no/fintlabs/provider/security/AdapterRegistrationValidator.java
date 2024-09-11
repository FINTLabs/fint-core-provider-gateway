package no.fintlabs.provider.security;

import lombok.RequiredArgsConstructor;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.provider.exception.InvalidAdapterCapabilityException;
import no.fintlabs.provider.security.resource.ResourceContext;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdapterRegistrationValidator {

    private final ResourceContext resourceContext;

    public void validateCapabilities(Set<AdapterCapability> capabilities) {
        capabilities.forEach(capability -> {
            String componentResource = "%s-%s-%s".formatted(capability.getDomainName(), capability.getPackageName(), capability.getResourceName());
            if (!resourceContext.getValidResources().contains(componentResource)) {
                throw new InvalidAdapterCapabilityException("Invalid capability resource: %s".formatted(componentResource));
            }
        });
    }

}
