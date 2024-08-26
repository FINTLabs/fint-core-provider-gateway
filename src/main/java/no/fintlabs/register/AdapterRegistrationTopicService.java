package no.fintlabs.register;

import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.config.ProviderProperties;
import no.fintlabs.kafka.TopicService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

import static no.fintlabs.register.TopicNamesConstants.*;

@Service
public class AdapterRegistrationTopicService {

    private final TopicService topicService;
    private final Map<String, Long> eventNamesToRetensionMap;

    public AdapterRegistrationTopicService(TopicService topicService, ProviderProperties providerProperties) {
        this.topicService = topicService;
        this.eventNamesToRetensionMap = initializeEventNamesToRetensionMap(providerProperties);
    }

    private Map<String, Long> initializeEventNamesToRetensionMap(ProviderProperties providerProperties) {
        return Map.of(
                HEARTBEAT_EVENT_NAME, providerProperties.getAdapterHeartbeatRetentionTimeMs(),
                ADAPTER_REGISTER_EVENT_NAME, providerProperties.getAdapterRegisterRetentionTimeMs(),
                ADAPTER_FULL_SYNC_EVENT_NAME, providerProperties.getAdapterFullSyncRetentionTimeMs(),
                ADAPTER_DELTA_SYNC_EVENT_NAME, providerProperties.getAdapterDeltaSyncRetentionTimeMs(),
                ADAPTER_DELETE_SYNC_EVENT_NAME, providerProperties.getAdapterDeleteSyncRetentionTimeMs()
        );
    }

    public void ensureTopics(AdapterContract adapterContract) {
        eventNamesToRetensionMap.forEach((eventName, retensionTimeInMs) -> ensureEventTopic(adapterContract.getOrgId(), eventName, retensionTimeInMs));
        ensureEntityTopics(adapterContract);
    }

    private void ensureEntityTopics(AdapterContract adapterContract) {
        adapterContract.getCapabilities().forEach(capability -> {
            long retensionTime = Duration.ofDays(capability.getFullSyncIntervalInDays()).toMillis();

            EntityTopicNameParameters topicNameParameters = EntityTopicNameParameters.builder()
                    .orgId(adapterContract.getOrgId())
                    .resource(getResourceName(capability))
                    .build();

            if (topicService.topicExists(topicNameParameters)) {
                if (topicService.topicHasDifferentRetensionTime(topicNameParameters, retensionTime)) {
                    topicService.ensureTopic(topicNameParameters, retensionTime);
                }
            } else {
                topicService.ensureTopic(topicNameParameters, retensionTime);
            }
        });
    }

    private void ensureEventTopic(String orgId, String eventName, Long retensionTime) {
        EventTopicNameParameters eventTopicNameParameters = EventTopicNameParameters.builder()
                .orgId(orgId)
                .eventName(eventName)
                .build();


        if (!topicService.topicExists(eventTopicNameParameters)) {
            topicService.ensureTopic(eventTopicNameParameters, retensionTime);
        }
    }

    private String getResourceName(AdapterCapability capability) {
        return "%s-%s-%s".formatted(capability.getDomainName(), capability.getPackageName(), capability.getResourceName());
    }

}
