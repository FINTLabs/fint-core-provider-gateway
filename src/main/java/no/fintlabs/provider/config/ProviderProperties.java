package no.fintlabs.provider.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class ProviderProperties {

    @Value("${fint.provider.adapter.heartbeat.retention-time-ms:86400000}")
    private long adapterHeartbeatRetentionTimeMs;

    @Value("${fint.provider.adapter.register.retention-time-ms:-1}")
    private long adapterRegisterRetentionTimeMs;

    @Value("${fint.provider.adapter.full-sync.retention-time-ms:86400000}")
    private long adapterFullSyncRetentionTimeMs;

    @Value("${fint.provider.adapter.delta-sync.retention-time-ms:86400000}")
    private long adapterDeltaSyncRetentionTimeMs;

    @Value("${fint.provider.adapter.delete-sync.retention-time-ms:86400000}")
    private long adapterDeleteSyncRetentionTimeMs;

}
