package no.fintlabs.provider.heartbeat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterHeartbeat;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINTLABS_NO;

@Slf4j
@RequiredArgsConstructor
@Service
public class HeartbeatService {

    private final HeartbeatKafkaProducer heartbeatKafkaProducer;

    public void beat(AdapterHeartbeat adapterHeartbeat) {
        log.debug("Heartbeat from adapter id: {}, orgId: {}, username: {}", adapterHeartbeat.getAdapterId(), adapterHeartbeat.getOrgId(), adapterHeartbeat.getUsername());
        heartbeatKafkaProducer.send(adapterHeartbeat, FINTLABS_NO);
    }
}
