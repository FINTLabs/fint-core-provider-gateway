package no.fintlabs.kafka.ensure;

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
        eventTopicService.ensureTopic(EventTopicNameParameters
                        .builder()
                        .orgId(adapterHeartbeat.getOrgId())
                        .eventName("adapter-health")
                        .build(),
                providerProperties.getAdapterHeartbeatRetentionTimeMs()
        );
    }

    public void ensureAdapterRegisterTopic(AdapterContract adapterContract) {
        eventTopicService.ensureTopic(EventTopicNameParameters
                        .builder()
                        .orgId(adapterContract.getOrgId())
                        .eventName("adapter-register")
                        .build(),
                providerProperties.getAdapterRegisterRetentionTimeMs()
        );
    }

    public void ensureAdapterFullSyncTopic(AdapterContract adapterContract) {
        eventTopicService.ensureTopic(EventTopicNameParameters
                        .builder()
                        .orgId(adapterContract.getOrgId())
                        .eventName("adapter-full-sync")
                        .build(),
                providerProperties.getAdapterFullSyncRetentionTimeMs()
        );
    }

    public void ensureAdapterDeltaSyncTopic(AdapterContract adapterContract) {
        eventTopicService.ensureTopic(EventTopicNameParameters
                        .builder()
                        .orgId(adapterContract.getOrgId())
                        .eventName("adapter-delta-sync")
                        .build(),
                providerProperties.getAdapterDeltaSyncRetentionTimeMs()
        );
    }
}
