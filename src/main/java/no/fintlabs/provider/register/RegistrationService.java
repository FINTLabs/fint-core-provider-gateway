package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.provider.security.AdapterRegistrationValidator;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegistrationService {

    private final AdapterContractProducer adapterContractProducer;
    private final ContractService contractService;
    private final AdapterRegistrationValidator adapterRegistrationValidator;
    private final AdapterRegistrationTopicService adapterRegistrationTopicService;

    public void register(AdapterContract adapterContract) {
        if (adapterContract.getCapabilities() != null && !adapterContract.getCapabilities().isEmpty()) {
        adapterRegistrationValidator.validateContract(adapterContract);
        adapterRegistrationTopicService.createCapabilityTopics(adapterContract);
        }
        adapterContractProducer.send(adapterContract);
        contractService.saveContract(adapterContract);
    }

}
