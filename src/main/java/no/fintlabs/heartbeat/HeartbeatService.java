package no.fintlabs.heartbeat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.resource.server.security.CorePrincipal;
import no.fintlabs.utils.AdapterRequestValidator;
import no.fintlabs.adapter.models.AdapterHeartbeat;
import no.fintlabs.kafka.FintCoreEventTopicService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class HeartbeatService {

    private final AdapterRequestValidator validator;
    private final FintCoreEventTopicService fintCoreEventTopicService;
    private final HeartbeatKafkaProducer heartbeatKafkaProducer;

    public void register(AdapterHeartbeat adapterHeartbeat, CorePrincipal corePrincipal) {
        log.debug("Heartbeat from adapter id: {}, orgIds: {}, username: {}", adapterHeartbeat.getAdapterId(), adapterHeartbeat.getOrgId(), adapterHeartbeat.getUsername());
        validator.validateOrgId(corePrincipal, adapterHeartbeat.getOrgId());
        validator.validateUsername(corePrincipal, adapterHeartbeat.getUsername());
        fintCoreEventTopicService.ensureAdapterHeartbeatTopic(adapterHeartbeat);
        heartbeatKafkaProducer.send(adapterHeartbeat, adapterHeartbeat.getOrgId());
    }
}
