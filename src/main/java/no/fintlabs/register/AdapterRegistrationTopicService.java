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
    private final AdapterContractContext adapterContractContext;

    public AdapterRegistrationTopicService(TopicService topicService, AdapterContractContext adapterContractContext, ProviderProperties providerProperties) {
        this.topicService = topicService;
        this.adapterContractContext = adapterContractContext;
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
        AdapterContract oldAdapterContract = adapterContractContext.get(adapterContract.getUsername());

        adapterContract.getCapabilities().forEach(capability -> {
            EntityTopicNameParameters topicNameParameters = EntityTopicNameParameters.builder()
                    .orgId(adapterContract.getOrgId())
                    .resource(getResourceName(capability))
                    .build();

            if (topicService.topicExists(topicNameParameters) && oldAdapterContract != null) {
                AdapterCapability oldCapability = getOldCapability(capability, oldAdapterContract);

                if (newCapabilityHasChanges(capability, oldCapability)) {
                    topicService.ensureTopic(topicNameParameters, Duration.ofDays(capability.getFullSyncIntervalInDays()).toMillis());
                }
            } else {
                topicService.ensureTopic(topicNameParameters, Duration.ofDays(capability.getFullSyncIntervalInDays()).toMillis());
            }
        });
    }

    private AdapterCapability getOldCapability(AdapterCapability newAdapterCapability, AdapterContract oldAdapterContract) {
        return oldAdapterContract.getCapabilities().stream()
                .filter(o -> o.getDomainName().equals(newAdapterCapability.getDomainName()) &&
                        o.getPackageName().equals(newAdapterCapability.getPackageName()) &&
                        o.getResourceName().equals(newAdapterCapability.getResourceName()))
                .findFirst()
                .orElse(null);
    }

    private boolean newCapabilityHasChanges(AdapterCapability capability, AdapterCapability oldCapability) {
        return oldCapability != null && capability.getFullSyncIntervalInDays() != oldCapability.getFullSyncIntervalInDays();
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
