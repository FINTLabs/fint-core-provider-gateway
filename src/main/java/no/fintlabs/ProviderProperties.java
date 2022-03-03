package no.fintlabs;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class ProviderProperties {
    @Value("${fint.provider.adapter.ping.retention-time-ms:86400000}")
    private long adapterPingRetentionTimeMs;

    @Value("${fint.provider.adapter.register.retention-time-ms:86400000}")
    private long adapterRegisterRetentionTimeMs;

    @Value("${fint.provider.adapter.scope:fint-adapter}")
    private String scope;



}
