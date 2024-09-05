package no.fintlabs.provider.kafka;

import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.provider.config.ProviderProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

import static no.fintlabs.provider.kafka.TopicNamesConstants.*;

@Component
public class EventTopicEnsurer {

    private final Map<String, Long> eventTopicNameRetentionMap;
    private final ProviderTopicService providerTopicService;

    public EventTopicEnsurer(ProviderProperties providerProperties, ProviderTopicService providerTopicService) {
        this.eventTopicNameRetentionMap = setEventTopicNameRetentionMap(providerProperties);
        this.providerTopicService = providerTopicService;
        ensureEventTopics();
    }

    private void ensureEventTopics() {
        eventTopicNameRetentionMap.forEach((eventName, retensionTimeInMs) -> {
            EventTopicNameParameters eventTopicNameParameters = EventTopicNameParameters.builder()
                    .orgId(FINTLABS_NO)
                    .domainContext(FINT_CORE)
                    .eventName(eventName)
                    .build();

            providerTopicService.ensureTopic(eventTopicNameParameters, retensionTimeInMs);
        });
    }

    private Map<String, Long> setEventTopicNameRetentionMap(ProviderProperties providerProperties) {
        return Map.of(
                HEARTBEAT_EVENT_NAME, providerProperties.getAdapterHeartbeatRetentionTimeMs(),
                ADAPTER_REGISTER_EVENT_NAME, providerProperties.getAdapterRegisterRetentionTimeMs(),
                ADAPTER_FULL_SYNC_EVENT_NAME, providerProperties.getAdapterFullSyncRetentionTimeMs(),
                ADAPTER_DELTA_SYNC_EVENT_NAME, providerProperties.getAdapterDeltaSyncRetentionTimeMs(),
                ADAPTER_DELETE_SYNC_EVENT_NAME, providerProperties.getAdapterDeleteSyncRetentionTimeMs()
        );
    }



}
