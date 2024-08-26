package no.fintlabs.provider.security;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AdapterContractContext {

    private Map<String, String> adapterIdToUsernameMap = new HashMap<>();

    public void add(String adapterId, String username) {
        adapterIdToUsernameMap.put(adapterId, username);
    }

    public boolean userCanAccessAdapter(String username, String adapterId) {
        return adapterIdToUsernameMap.getOrDefault(adapterId, "").equals(username);
    }
}
