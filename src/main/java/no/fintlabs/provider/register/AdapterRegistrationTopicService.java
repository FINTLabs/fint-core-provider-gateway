package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.provider.kafka.ProviderTopicService;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINT_CORE;

@Service
@RequiredArgsConstructor
public class AdapterRegistrationTopicService {

    private final ProviderTopicService providerTopicService;

    public void ensureCapabilityTopics(AdapterContract adapterContract) {
        adapterContract.getCapabilities().forEach(capability -> {
            long retentionTime = Duration.ofDays(7).toMillis();
            EntityTopicNameParameters topicNameParameters = createTopicNameParameters(adapterContract.getOrgId(), capability);

            if (providerTopicService.topicExists(topicNameParameters)) {
                if (providerTopicService.topicHasDifferentRetentionTime(topicNameParameters, retentionTime)) {
                    providerTopicService.ensureTopic(topicNameParameters, retentionTime);
                }
            } else {
                providerTopicService.ensureTopic(topicNameParameters, retentionTime);
            }
        });
    }

    private EntityTopicNameParameters createTopicNameParameters(String org, AdapterCapability adapterCapability) {
        return EntityTopicNameParameters.builder()
                .orgId(org.replace(".", "-"))
                .domainContext(FINT_CORE)
                .resource(getResourceName(adapterCapability))
                .build();
    }

    private String getResourceName(AdapterCapability capability) {
        return "%s-%s-%s".formatted(capability.getDomainName(), capability.getPackageName(), capability.getResourceName());
    }

}
