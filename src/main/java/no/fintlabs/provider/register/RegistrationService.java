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
    private final AdapterRegistrationTopicService adapterRegistrationTopicService;
    private final ContractJpaRepository contractJpaRepository;

    public void register(AdapterContract adapterContract) {
        adapterRegistrationValidator.validateCapabilities(adapterContract.getCapabilities());
        //adapterRegistrationTopicService.createCapabilityTopics(adapterContract);
        //adapterContractProducer.send(adapterContract);
        adapterContractContext.saveOrUpdateContract(adapterContract);
    }

}
