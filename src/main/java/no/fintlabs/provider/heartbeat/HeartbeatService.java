package no.fintlabs.provider.heartbeat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterHeartbeat;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class HeartbeatService {

    private final HeartbeatKafkaProducer heartbeatKafkaProducer;

    public void beat(AdapterHeartbeat adapterHeartbeat) {
        log.debug("Heartbeat from adapter id: {}, orgId: {}, username: {}", adapterHeartbeat.getAdapterId(), adapterHeartbeat.getOrgId(), adapterHeartbeat.getUsername());
        heartbeatKafkaProducer.send(adapterHeartbeat);
    }
}
