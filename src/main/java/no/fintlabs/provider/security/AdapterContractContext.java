package no.fintlabs.provider.security;

import no.fintlabs.adapter.models.AdapterContract;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class AdapterContractContext {

    private final Map<String, String> adapterIdToUsernameMap = new HashMap<>();
    private final Map<String, Set<String>> adapterIdValidCapabilities = new HashMap<>();

    public Set<String> getAdapterIds() {
        return adapterIdToUsernameMap.keySet();
    }

    public boolean adapterCanPerformCapability(String adapterId, String domainName, String packageName, String entityName) {
        String compatibilityLink = "%s/%s/%s".formatted(domainName, packageName, entityName).toLowerCase();
        return adapterIdValidCapabilities.get(adapterId).contains(compatibilityLink);
    }

    public void add(AdapterContract adapterContract) {
        adapterIdToUsernameMap.put(adapterContract.getAdapterId(), adapterContract.getUsername());
        addAdapterIdValidCapabilities(adapterContract);
    }

    public boolean userCanAccessAdapter(String username, String adapterId) {
        return adapterIdToUsernameMap.getOrDefault(adapterId, "").equals(username);
    }

    private void addAdapterIdValidCapabilities(AdapterContract adapterContract) {
        adapterIdValidCapabilities.put(adapterContract.getAdapterId(), new HashSet<>());
        adapterContract.getCapabilities().forEach(capability ->
                adapterIdValidCapabilities.get(adapterContract.getAdapterId()).add(
                        "%s/%s/%s".formatted(
                                        capability.getDomainName(),
                                        capability.getPackageName(),
                                        capability.getResourceName())
                                .toLowerCase()
                ));
    }
}
