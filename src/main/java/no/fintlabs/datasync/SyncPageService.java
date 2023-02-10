package no.fintlabs.datasync;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.DeleteSyncPageOfObject;
import no.fintlabs.adapter.models.DeltaSyncPageOfObject;
import no.fintlabs.adapter.models.FullSyncPageOfObject;
import no.fintlabs.adapter.models.SyncPage;
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
    private final DeleteSyncProducerKafka deleteSyncProducer;

    public SyncPageService(
            EntityProducerKafka entityProducerKafka,
            FullSyncProducerKafka fullSyncProducer,
            DeltaSyncProducerKafka deltaSyncProducer,
            DeleteSyncProducerKafka deleteSyncProducer) {
        this.entityProducerKafka = entityProducerKafka;
        this.fullSyncProducer = fullSyncProducer;
        this.deltaSyncProducer = deltaSyncProducer;
        this.deleteSyncProducer = deleteSyncProducer;
    }

    public void doFullSync(FullSyncPageOfObject page, String domain, String packageName, String entity) {
        Instant start = Instant.now();

        fullSyncProducer.sendAndGet(page.getMetadata());
        sendEntities(page, domain, packageName, entity);

        Instant finish = Instant.now();
        Duration timeElapsed = Duration.between(start, finish);
        logDuration("full", page.getMetadata().getCorrId(), timeElapsed);
    }

    public void doDeltaSync(DeltaSyncPageOfObject page, String domain, String packageName, String entity) {
        Instant start = Instant.now();

        deltaSyncProducer.sendAndGet(page.getMetadata());
        sendEntities(page, domain, packageName, entity);

        Instant finish = Instant.now();
        Duration timeElapsed = Duration.between(start, finish);
        logDuration("delta", page.getMetadata().getCorrId(), timeElapsed);
    }

    public void doDeleteSync(DeleteSyncPageOfObject page, String domain, String packageName, String entity) {
        Instant start = Instant.now();

        page.getResources().forEach(syncPageEntry -> syncPageEntry.setResource(null));

        deleteSyncProducer.send(page.getMetadata());
        sendEntities(page, domain, packageName, entity);

        Instant finish = Instant.now();
        Duration timeElapsed = Duration.between(start, finish);
        logDuration("delete", page.getMetadata().getCorrId(), timeElapsed);
    }

    private <T> void sendEntities(SyncPage<Object> page, String domain, String packageName, String entity) {
        page.getResources().forEach(
                syncPageEntry -> {
                    try {
                        entityProducerKafka.sendEntity(
                                page.getMetadata().getOrgId(),
                                domain,
                                packageName,
                                entity,
                                syncPageEntry
                        ).get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error(e.getMessage());
                    }
                }
        );
    }

    private void logDuration(String dataSyncMethod, String corrId, Duration timeTaken) {
        log.info("End {} sync ({}). It took {} hours, {} minutes, {} seconds to complete",
                dataSyncMethod,
                corrId,
                timeTaken.toHoursPart(),
                timeTaken.toMinutesPart(),
                timeTaken.toSecondsPart()
        );
    }

}
