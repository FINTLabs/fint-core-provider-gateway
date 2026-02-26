package no.fintlabs.provider.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ProviderProperties {

    @Value("${fint.provider.adapter.heartbeat.retention-time-ms:86400000}")
    private long adapterHeartbeatRetentionTimeMs;

    // TODO: Vi bør kanskje ta i bruk table eller en annen persistering enn en Queue i Kafka?
    @Value("${fint.provider.adapter.register.retention-time-ms:9223372036854775807}")
    private long adapterRegisterRetentionTimeMs;

    @Value("${fint.provider.adapter.full-sync.retention-time-ms:86400000}")
    private long adapterFullSyncRetentionTimeMs;

    @Value("${fint.provider.adapter.delta-sync.retention-time-ms:86400000}")
    private long adapterDeltaSyncRetentionTimeMs;

    @Value("${fint.provider.adapter.delete-sync.retention-time-ms:86400000}")
    private long adapterDeleteSyncRetentionTimeMs;

    @Getter
    @Value("${fint.provider.pod-url:http://fint-core-provider-gateway:8080}")
    private String podUrl;

    public Duration getAdapterHeartbeatRetentionTime() {
        return Duration.ofMillis(adapterHeartbeatRetentionTimeMs);
    }

    public Duration getAdapterRegisterRetentionTime() {
        return Duration.ofMillis(adapterRegisterRetentionTimeMs);
    }

    public Duration getAdapterFullSyncRetentionTime() {
        return Duration.ofMillis(adapterFullSyncRetentionTimeMs);
    }

    public Duration getAdapterDeltaSyncRetentionTime() {
        return Duration.ofMillis(adapterDeltaSyncRetentionTimeMs);
    }

    public Duration getAdapterDeleteSyncRetentionTime() {
        return Duration.ofMillis(adapterDeleteSyncRetentionTimeMs);
    }

}
