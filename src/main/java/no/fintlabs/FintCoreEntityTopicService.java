package no.fintlabs;

import no.fintlabs.kafka.entity.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.EntityTopicService;
import no.fintlabs.model.AdapterContract;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class FintCoreEntityTopicService {
    private final EntityTopicService entityTopicService;

    public FintCoreEntityTopicService(EntityTopicService entityTopicService) {
        this.entityTopicService = entityTopicService;
    }

    public void ensureEntityTopic(AdapterContract.AdapterCapability adapterCapability, String orgId, long retentionTimeMs) {
        entityTopicService.ensureTopic(EntityTopicNameParameters
                        .builder()
                        .orgId(orgId)
                        .domainContext("fint-core")
                        .resource(String.format(
                                        "%s-%s-%s",
                                        adapterCapability.getDomain(),
                                        adapterCapability.getPackageName(),
                                        adapterCapability.getClazz()
                                )
                        )
                        .build(),
                retentionTimeMs);
    }

    public void ensureAdapterEntityTopics(AdapterContract adapterContract) {
        adapterContract.getCapability()
                .forEach(capability -> ensureEntityTopic(capability,
                                adapterContract.getOrgId(),
                                TimeUnit.DAYS.toMillis(capability.getFullSyncIntervalInDays())
                        )
                );
    }
}
