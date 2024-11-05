package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.provider.security.AdapterContractContext;
import no.fintlabs.provider.security.AdapterRegistrationValidator;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINTLABS_NO;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegistrationService {

    private final AdapterContractProducer adapterContractProducer;
    private final AdapterContractContext adapterContractContext;
    private final AdapterRegistrationValidator adapterRegistrationValidator;
    private final AdapterRegistrationTopicService adapterRegistrationTopicService;

    public void register(AdapterContract adapterContract) {
        adapterRegistrationValidator.validateCapabilities(adapterContract.getCapabilities());
        adapterRegistrationTopicService.ensureCapabilityTopics(adapterContract);
        adapterContractProducer.send(adapterContract, FINTLABS_NO);
        adapterContractContext.add(adapterContract);
        log.info("New adapter has registered: {}", adapterContract.getAdapterId());
    }

}
