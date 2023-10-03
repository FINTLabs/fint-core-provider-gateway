package no.fintlabs.register;

import no.fintlabs.adapter.models.AdapterCapability;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class FintCoreEntityTopicService {
    private final EntityTopicService entityTopicService;

    public FintCoreEntityTopicService(EntityTopicService entityTopicService) {
        this.entityTopicService = entityTopicService;
    }

    public void ensureAdapterEntityTopics(AdapterContract adapterContract) {
        for (AdapterCapability capability : adapterContract.getCapabilities()) {
            ensureEntityTopic(capability, adapterContract.getOrgId(), TimeUnit.DAYS.toMillis(capability.getFullSyncIntervalInDays()));
        }
    }

    private void ensureEntityTopic(AdapterCapability adapterCapability, String orgId, long retentionTimeMs) {
        String resource = "%s-%s-%s".formatted(adapterCapability.getDomainName(), adapterCapability.getPackageName(), adapterCapability.getResourceName());
        entityTopicService.ensureTopic(EntityTopicNameParameters
                        .builder()
                        .orgId(orgId)
                        .resource(resource)
                        .build(),
                retentionTimeMs);
    }
}
