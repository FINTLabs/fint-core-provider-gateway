package no.fintlabs.provider.register;

import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.provider.config.ProviderProperties;
import no.fintlabs.provider.kafka.ProviderTopicService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

import static no.fintlabs.provider.kafka.TopicNamesConstants.*;

@Service
public class AdapterRegistrationTopicService {

    private final ProviderTopicService topicService;
    private final Map<String, Long> eventNamesToRetensionMap;

    public AdapterRegistrationTopicService(ProviderTopicService topicService, ProviderProperties providerProperties) {
        this.topicService = topicService;
        this.eventNamesToRetensionMap = initializeEventNamesToRetensionMap(providerProperties);
    }

    // TODO: Trenger vi å gjøre dette for hver org? AdapterContract har informasjon om orginsasjonen sin så hvorfor skal det være forskjellig topics?
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
        eventNamesToRetensionMap.forEach((eventName, retensionTimeInMs) ->
                ensureEventTopic(adapterContract.getOrgId(), eventName, retensionTimeInMs)
        );
        ensureEntityTopics(adapterContract);
    }

    private void ensureEntityTopics(AdapterContract adapterContract) {
        adapterContract.getCapabilities().forEach(capability -> {
            long retensionTime = Duration.ofDays(capability.getFullSyncIntervalInDays()).toMillis();

            EntityTopicNameParameters topicNameParameters = EntityTopicNameParameters.builder()
                    .orgId(adapterContract.getOrgId())
                    .domainContext(FINT_CORE)
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
                .domainContext(FINT_CORE)
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
