package no.fintlabs.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.kafka.FintCoreEventTopicService;
import no.fintlabs.utils.AdapterRequestValidator;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegisterService {

    private final AdapterRequestValidator validator;
    private final FintCoreEventTopicService fintCoreEventTopicService;
    private final RegisterKafkaProducer registerKafkaProducer;
    private final FintCoreEntityTopicService fintCoreEntityTopicService;

    public void register(AdapterContract adapterContract, CorePrincipal corePrincipal) {
        log.info("Adapter registered {}", adapterContract);

        validator.validateOrgId(corePrincipal, adapterContract.getOrgId());
        validator.validateUsername(corePrincipal, adapterContract.getUsername());

        fintCoreEventTopicService.ensureAdapterRegisterTopic(adapterContract);

        registerKafkaProducer.send(adapterContract, adapterContract.getOrgId());
        fintCoreEntityTopicService.ensureAdapterEntityTopics(adapterContract);
        fintCoreEventTopicService.ensureAdapterFullSyncTopic(adapterContract);
        fintCoreEventTopicService.ensureAdapterDeltaSyncTopic(adapterContract);
        fintCoreEventTopicService.ensureAdapterDeleteSyncTopic(adapterContract);
    }
}
