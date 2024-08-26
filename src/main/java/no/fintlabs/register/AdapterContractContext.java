package no.fintlabs.register;

import jakarta.annotation.Nullable;
import no.fintlabs.adapter.models.AdapterContract;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdapterContractContext {

    private final Map<String, AdapterContract> adapterContracts = new HashMap<>();

    public void add(AdapterContract adapterContract) {
        adapterContracts.put(adapterContract.getUsername(), adapterContract);
    }

    @Nullable
    public AdapterContract get(String username) {
        return adapterContracts.get(username);
    }

}
