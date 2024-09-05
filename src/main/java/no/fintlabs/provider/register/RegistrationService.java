package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.provider.security.AdapterContractContext;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINTLABS_NO;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegistrationService {

    private final AdapterContractProducer adapterContractProducer;
    private final AdapterRegistrationTopicService adapterRegistrationTopicService;
    private final AdapterContractContext adapterContractContext;

    public void register(AdapterContract adapterContract) {
        adapterContractProducer.send(adapterContract, FINTLABS_NO);
        adapterContractContext.add(adapterContract);
        log.info("New adapter has registered: {}", adapterContract.getAdapterId());
    }

}
