package no.fintlabs.provider.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import no.fintlabs.provider.config.ProviderProperties;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import no.novari.kafka.topic.name.TopicNamePrefixParameters;
import org.springframework.stereotype.Component;

import java.util.Map;

import static no.fintlabs.provider.kafka.TopicNamesConstants.ADAPTER_DELETE_SYNC_EVENT_NAME;
import static no.fintlabs.provider.kafka.TopicNamesConstants.ADAPTER_DELTA_SYNC_EVENT_NAME;
import static no.fintlabs.provider.kafka.TopicNamesConstants.ADAPTER_FULL_SYNC_EVENT_NAME;
import static no.fintlabs.provider.kafka.TopicNamesConstants.ADAPTER_REGISTER_EVENT_NAME;
import static no.fintlabs.provider.kafka.TopicNamesConstants.HEARTBEAT_EVENT_NAME;

@Component
@RequiredArgsConstructor
public class EventTopicEnsurer {

    private final ProviderTopicService providerTopicService;
    private final ProviderProperties providerProperties;

    @PostConstruct
    public void init() {
        ensureEventTopics(providerProperties);
    }

    private void ensureEventTopics(ProviderProperties providerProperties) {
        Map.of(
                HEARTBEAT_EVENT_NAME, providerProperties.getAdapterHeartbeatRetentionTime(),
                ADAPTER_REGISTER_EVENT_NAME, providerProperties.getAdapterRegisterRetentionTime(),
                ADAPTER_FULL_SYNC_EVENT_NAME, providerProperties.getAdapterFullSyncRetentionTime(),
                ADAPTER_DELTA_SYNC_EVENT_NAME, providerProperties.getAdapterDeltaSyncRetentionTime(),
                ADAPTER_DELETE_SYNC_EVENT_NAME, providerProperties.getAdapterDeleteSyncRetentionTime()
        ).forEach((eventName, retentionTime) -> providerTopicService.createOrModifyTopic(
                EventTopicNameParameters.builder()
                        .topicNamePrefixParameters(
                                TopicNamePrefixParameters
                                        .stepBuilder()
                                        .orgIdApplicationDefault()
                                        .domainContextApplicationDefault()
                                        .build()
                        )
                        .eventName(eventName)
                        .build(),
                retentionTime
        ));
    }

}
