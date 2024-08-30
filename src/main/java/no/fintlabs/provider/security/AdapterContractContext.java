package no.fintlabs.provider.security;

import no.fintlabs.adapter.models.AdapterContract;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AdapterContractContext {

    private final Map<String, String> adapterIdToUsernameMap = new HashMap<>();

    public void add(String adapterId, String username) {
        adapterIdToUsernameMap.put(adapterId, username);
    public void add(AdapterContract adapterContract) {
        adapterIdToUsernameMap.put(adapterContract.getAdapterId(), adapterContract.getUsername());
        addAdapterIdValidCapabilities(adapterContract);
    }

    public boolean userCanAccessAdapter(String username, String adapterId) {
        return adapterIdToUsernameMap.getOrDefault(adapterId, "").equals(username);
    }
}
