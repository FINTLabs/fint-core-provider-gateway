package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.provider.security.AdapterContractContext;
import no.fintlabs.provider.security.AdapterRegistrationValidator;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegistrationService {

    private final AdapterContractProducer adapterContractProducer;
    private final AdapterContractContext adapterContractContext;
    private final AdapterRegistrationValidator adapterRegistrationValidator;

    public void register(AdapterContract adapterContract) {
        adapterRegistrationValidator.validateCapabilities(adapterContract.getCapabilities());
        adapterContractProducer.send(adapterContract, adapterContract.getOrgId());
        adapterContractContext.add(adapterContract);
        log.info("New adapter has registered: {}", adapterContract.getAdapterId());
    }

}
