package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.adapter.models.AdapterPing;
import no.fintlabs.adapter.models.FullSyncPage;
import no.fintlabs.adapter.models.FullSyncPageMapOfObject;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.FintKafkaEntityProducerFactory;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.EventTopicNameParameters;
import no.fintlabs.kafka.event.FintKafkaEventProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class FintCoreKafkaAdapterService {
    private final EventProducer<AdapterPing> adapterPingEventProducer;
    private final EventProducer<FullSyncPage.Metadata> adapterFullSyncStatusEventProducer;
    private final EventProducer<AdapterContract> adapterContractEventProducer;
    private final EntityProducer<Object> entityProducer;
    private final FintKafkaEntityProducerFactory entityProducerFactory;
    private final FintKafkaEventProducerFactory eventProducerFactory;

    public FintCoreKafkaAdapterService(FintKafkaEntityProducerFactory entityProducerFactory, FintKafkaEventProducerFactory eventProducerFactory) {
        this.adapterPingEventProducer = eventProducerFactory.createProducer(AdapterPing.class);
        this.adapterContractEventProducer = eventProducerFactory.createProducer(AdapterContract.class);
        this.adapterFullSyncStatusEventProducer = eventProducerFactory.createProducer(FullSyncPage.Metadata.class);
        this.entityProducer = entityProducerFactory.createProducer(Object.class);
        this.entityProducerFactory = entityProducerFactory;
        this.eventProducerFactory = eventProducerFactory;
    }


    public void ping(AdapterPing adapterPing) {
        //EventProducer<AdapterPing> adapterPingEventProducer = eventProducerFactory.createProducer(AdapterPing.class);
        adapterPingEventProducer.send(
                EventProducerRecord.<AdapterPing>builder()
                        .topicNameParameters(EventTopicNameParameters
                                .builder()
                                .orgId(adapterPing.getOrgId())
                                .domainContext("fint-core")
                                .eventName("adapter-health")
                                .build())
                        .value(adapterPing)
                        .build()
        );
    }

    public void register(AdapterContract adapterContract) {
        adapterContractEventProducer.send(
                EventProducerRecord.<AdapterContract>builder()
                        .topicNameParameters(EventTopicNameParameters
                                .builder()
                                .orgId(adapterContract.getOrgId())
                                .domainContext("fint-core")
                                .eventName("adapter-register")
                                .build())
                        .value(adapterContract)
                        .build()
        );
    }


    public void sendFullSyncStatus(FullSyncPage.Metadata metadata) {
        try {
            adapterFullSyncStatusEventProducer.send(
                    EventProducerRecord.<FullSyncPage.Metadata>builder()
                            .topicNameParameters(EventTopicNameParameters
                                    .builder()
                                    .orgId(metadata.getOrgId())
                                    .domainContext("fint-core")
                                    .eventName("adapter-full-sync")
                                    .build())
                            .value(metadata)
                            .build()
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
        }
    }

    public void sendEntity(FullSyncPageMapOfObject entities, String domain, String packageName, String entity) {

        Instant start = Instant.now();
        entities.getResources().forEach(
                resource -> {
                    try {
                        entity(
                                entities.getMetadata().getOrgId(),
                                domain,
                                packageName,
                                entity,
                                resource
                        ).get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error(e.getMessage());
                        //throw new InvalidOrgId("");
                    }
                }
        );
        Instant finish = Instant.now();
        Duration timeElapsed = Duration.between(start, finish);
        log.info("End full sync. It took {} hours, {} minutes, {} seconds to complete", timeElapsed.toHoursPart(), timeElapsed.toMinutesPart(), timeElapsed.toSecondsPart());
    }

    private ListenableFuture<SendResult<String, Object>> entity(String orgId, String domain, String packageName, String entityName, Object entity) {
        return entityProducer.send(
                EntityProducerRecord.builder()
                        .topicNameParameters(EntityTopicNameParameters
                                .builder()
                                .orgId(orgId)
                                .domainContext("fint-core")
                                .resource(String.format("%s-%s-%s", domain, packageName, entityName))
                                .build())
                        .value(entity)
                        .key(getKey(entity))
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    private String getKey(Object resource) {
        HashMap<String, ?> links = (HashMap<String, ?>) ((HashMap<String, ?>)resource).get("_links");
        List<HashMap<String, String>> selfLinks = (List<HashMap<String, String>>) links.get("self");
        List<String> selfLinksList = selfLinks.stream()
                .filter(o -> o.containsKey("href"))
                .map(o -> o.get("href"))
                .map(k -> k.replaceFirst("^https:/\\/.+\\.felleskomponent.no", ""))
                .sorted()
                .toList();
        return selfLinksList.get(0);
    }
}
