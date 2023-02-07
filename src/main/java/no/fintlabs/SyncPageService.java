package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.DeltaSyncPageOfObject;
import no.fintlabs.adapter.models.FullSyncPageOfObject;
import no.fintlabs.adapter.models.SyncPage;
import no.fintlabs.kafka.DeltaSyncProducerKafka;
import no.fintlabs.kafka.EntityProducerKafka;
import no.fintlabs.kafka.FullSyncProducerKafka;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class SyncPageService {
    private final EntityProducerKafka entityProducerKafka;
    private final FullSyncProducerKafka fullSyncProducer;
    private final DeltaSyncProducerKafka deltaSyncProducer;

    public SyncPageService(
            EntityProducerKafka entityProducerKafka,
            FullSyncProducerKafka fullSyncProducer,
            DeltaSyncProducerKafka deltaSyncProducer) {
        this.entityProducerKafka = entityProducerKafka;
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
                        entityProducerKafka.sendEntity(
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
}
