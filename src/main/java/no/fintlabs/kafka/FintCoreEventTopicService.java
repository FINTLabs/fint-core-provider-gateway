package no.fintlabs.kafka;

import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.adapter.models.AdapterHeartbeat;
import no.fintlabs.config.ProviderProperties;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

@Service
public class FintCoreEventTopicService {
    private final EventTopicService eventTopicService;
    private final ProviderProperties providerProperties;

    public FintCoreEventTopicService(EventTopicService eventTopicService, ProviderProperties providerProperties) {
        this.eventTopicService = eventTopicService;
        this.providerProperties = providerProperties;
    }

    public void ensureAdapterHeartbeatTopic(AdapterHeartbeat adapterHeartbeat) {
        ensureTopic(adapterHeartbeat.getOrgId(), "adapter-health", providerProperties.getAdapterHeartbeatRetentionTimeMs());
    }

    public void ensureAdapterRegisterTopic(AdapterContract adapterContract) {
        ensureTopic(adapterContract, "adapter-register", providerProperties.getAdapterRegisterRetentionTimeMs());
    }

    public void ensureAdapterFullSyncTopic(AdapterContract adapterContract) {
        ensureTopic(adapterContract, "adapter-full-sync", providerProperties.getAdapterFullSyncRetentionTimeMs());
    }

    public void ensureAdapterDeltaSyncTopic(AdapterContract adapterContract) {
        ensureTopic(adapterContract, "adapter-delta-sync", providerProperties.getAdapterDeltaSyncRetentionTimeMs());
    }

    private void ensureTopic(AdapterContract adapterContract, String eventName, long retentionTime) {
        ensureTopic(adapterContract.getOrgId(), eventName, retentionTime);
    }

    private void ensureTopic(String orgId, String eventName, long retentionTime) {
        eventTopicService.ensureTopic(EventTopicNameParameters
                        .builder()
                        .orgId(orgId)
                        .eventName(eventName)
                        .build(),
                retentionTime
        );
    }
}
