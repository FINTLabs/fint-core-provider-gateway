package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.provider.kafka.ProviderTopicService;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINT_CORE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdapterRegistrationTopicService {

    private final ProviderTopicService topicService;

    public void ensureTopics(AdapterContract adapterContract) {
        adapterContract.getCapabilities().forEach(capability -> {
            topicService.ensureTopic(
                    EntityTopicNameParameters.builder()
                            .orgId(adapterContract.getOrgId())
                            .domainContext(FINT_CORE)
                            .resource(getResourceName(capability))
                            .build(),
                    Duration.ofDays(capability.getFullSyncIntervalInDays()).toMillis()
            );
        });
    }

    private String getResourceName(AdapterCapability capability) {
        return "%s-%s-%s".formatted(capability.getDomainName(), capability.getPackageName(), capability.getResourceName());
    }

}
