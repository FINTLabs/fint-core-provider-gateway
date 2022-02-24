package no.fintlabs;

import no.fintlabs.kafka.event.EventTopicNameParameters;
import no.fintlabs.kafka.event.EventTopicService;
import no.fintlabs.model.AdapterPing;
import no.fintlabs.model.AdapterContract;
import org.springframework.stereotype.Service;

@Service
public class FintCoreEventTopicService {
    private final EventTopicService eventTopicService;
    private final ProviderProperties providerProperties;

    public FintCoreEventTopicService(EventTopicService eventTopicService, ProviderProperties providerProperties) {
        this.eventTopicService = eventTopicService;
        this.providerProperties = providerProperties;
    }

    public String ensureAdapterPingTopic(AdapterPing adapterPing) {
        return eventTopicService.ensureTopic(EventTopicNameParameters
                        .builder()
                        .orgId(adapterPing.getOrgId())
                        .domainContext("fint-core")
                        .eventName("adapter-health")
                        .build(),
                providerProperties.getAdapterPingRetentionTimeMs()
        );
    }

    public String ensureAdapterRegisterTopic(AdapterContract adapterContract) {
        return eventTopicService.ensureTopic(EventTopicNameParameters
                        .builder()
                        .orgId(adapterContract.getOrgId())
                        .domainContext("fint-core")
                        .eventName("adapter-register")
                        .build(),
                providerProperties.getAdapterRegisterRetentionTimeMs()
        );
    }
}
