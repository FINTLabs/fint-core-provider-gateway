package no.fintlabs.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.utils.AdapterRequestValidator;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.FintCoreEventTopicService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegisterService {

    private final AdapterRequestValidator validator;
    private final FintCoreEventTopicService fintCoreEventTopicService;
    private final RegisterKafkaProducer registerKafkaProducer;
    private final FintCoreEntityTopicService fintCoreEntityTopicService;

    public void register(AdapterContract adapterContract, Jwt jwt) {
        log.info("Adapter registered {}", adapterContract);

        validator.validateOrgId(jwt, adapterContract.getOrgId());
        validator.validateUsername(jwt, adapterContract.getUsername());

        fintCoreEventTopicService.ensureAdapterRegisterTopic(adapterContract);

        registerKafkaProducer.send(adapterContract);
        fintCoreEntityTopicService.ensureAdapterEntityTopics(adapterContract);
        fintCoreEventTopicService.ensureAdapterFullSyncTopic(adapterContract);
        fintCoreEventTopicService.ensureAdapterDeltaSyncTopic(adapterContract);
    }
}
