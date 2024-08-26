package no.fintlabs.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.utils.AdapterRequestValidator;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdaterRegistrationService {

    private final AdapterRequestValidator validator;
    private final RegisterKafkaProducer registerKafkaProducer;
    private final AdapterRegistrationTopicService adapterRegistrationTopicService;

    public void register(AdapterContract adapterContract, CorePrincipal corePrincipal) {
        validateAdapterContract(corePrincipal, adapterContract);
        adapterRegistrationTopicService.ensureTopics(adapterContract);
        registerKafkaProducer.send(adapterContract, adapterContract.getOrgId());
        log.info("Adapter registered {}", adapterContract);
    }

    private void validateAdapterContract(CorePrincipal corePrincipal, AdapterContract adapterContract) {
        validator.validateOrgId(corePrincipal, adapterContract.getOrgId());
        validator.validateUsername(corePrincipal, adapterContract.getUsername());
    }

}
