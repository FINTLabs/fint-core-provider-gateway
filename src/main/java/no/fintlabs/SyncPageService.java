package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.*;
import no.fintlabs.kafka.DeltaSyncProducer;
import no.fintlabs.kafka.FullSyncProducer;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.event.EventProducerFactory;
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
public class SyncPageService {
    private final EntityProducer<Object> entityProducer;
    private final FullSyncProducer fullSyncProducer;
    private final DeltaSyncProducer deltaSyncProducer;

    public SyncPageService(
            EntityProducerFactory entityProducerFactory,
            EventProducerFactory eventProducerFactory,
            FullSyncProducer fullSyncProducer,
            DeltaSyncProducer deltaSyncProducer) {
        this.entityProducer = entityProducerFactory.createProducer(Object.class);
        this.fullSyncProducer = fullSyncProducer;
        this.deltaSyncProducer = deltaSyncProducer;
    }

    public void doFullSync(FullSyncPageOfObject page, String domain, String packageName, String entity) {
        Instant start = Instant.now();

        fullSyncProducer.sendAndGet(page.getMetadata());
        sendEntities(page, domain, packageName, entity);

        Instant finish = Instant.now();
        Duration timeElapsed = Duration.between(start, finish);
        log.info("End full sync ({}) for page {}. It took {}:{}:{} to complete ({})",
                page.getMetadata().getOrgId(),
                page.getMetadata().getPage(),
                String.format("%02d", timeElapsed.toHoursPart()),
                String.format("%02d", timeElapsed.toMinutesPart()),
                String.format("%02d", timeElapsed.toSecondsPart()),
                page.getMetadata().getCorrId()
        );
    }

    public void doDeltaSync(DeltaSyncPageOfObject page, String domain, String packageName, String entity) {
        Instant start = Instant.now();

        deltaSyncProducer.sendAndGet(page.getMetadata());
        sendEntities(page, domain, packageName, entity);

        Instant finish = Instant.now();
        Duration timeElapsed = Duration.between(start, finish);
        log.info("End delta sync ({}). It took {} hours, {} minutes, {} seconds to complete",
                page.getMetadata().getCorrId(),
                timeElapsed.toHoursPart(),
                timeElapsed.toMinutesPart(),
                timeElapsed.toSecondsPart()
        );
    }

    private <T> void sendEntities(SyncPage<Object> page, String domain, String packageName, String entity) {

        page.getResources().forEach(
                resource -> {
                    try {
                        sendEntity(
                                page.getMetadata().getOrgId(),
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
    }

    public ListenableFuture<SendResult<String, Object>> sendEntity(String orgId, String domain, String packageName, String entityName, SyncPageEntry<Object> entity) {
        return entityProducer.send(
                EntityProducerRecord.builder()
                        .topicNameParameters(EntityTopicNameParameters
                                .builder()
                                .orgId(orgId)
                                .resource(String.format("%s-%s-%s", domain, packageName, entityName))
                                .build())
                        .key(entity.getIdentifier())
                        .value(entity.getResource())
                        .build()
        );
    }

    @SuppressWarnings("unchecked")
    private String getKey(Object resource) {
        HashMap<String, ?> links = (HashMap<String, ?>) ((HashMap<String, ?>) resource).get("_links");
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
