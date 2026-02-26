package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.provider.kafka.ProviderTopicService;
import no.novari.kafka.topic.name.EntityTopicNameParameters;
import no.novari.kafka.topic.name.TopicNamePrefixParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AdapterRegistrationTopicService {

    private final ProviderTopicService providerTopicService;

    public void ensureCapabilityTopics(AdapterContract adapterContract) {
        adapterContract.getCapabilities().forEach(capability -> {
            Duration retentionTime = Duration.ofDays(7);
            EntityTopicNameParameters topicNameParameters = createTopicNameParameters(adapterContract.getOrgId(), capability);

            if (providerTopicService.topicExists(topicNameParameters)) {
                if (providerTopicService.topicHasDifferentRetentionTime(topicNameParameters, retentionTime)) {
                    providerTopicService.createOrModifyTopic(topicNameParameters, retentionTime);
                }
            } else {
                providerTopicService.createOrModifyTopic(topicNameParameters, retentionTime);
            }
        });
    }

    private EntityTopicNameParameters createTopicNameParameters(String org, AdapterCapability adapterCapability) {
        return EntityTopicNameParameters.builder()
                .topicNamePrefixParameters(
                        TopicNamePrefixParameters
                                .stepBuilder()
                                .orgId(org.replace(".", "-"))
                                .domainContextApplicationDefault()
                                .build()
                )
                .resourceName(getResourceName(adapterCapability))
                .build();
    }

    private String getResourceName(AdapterCapability capability) {
        return "%s-%s-%s".formatted(capability.getDomainName(), capability.getPackageName(), capability.getResourceName());
    }

}
