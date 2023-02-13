package no.fintlabs.datasync;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class SyncPageService {
    private final EntityProducerKafka entityProducerKafka;
    private final FullSyncProducerKafka fullSyncProducerKafka;
    private final DeltaSyncProducerKafka deltaSyncProducerKafka;
    private final DeleteSyncProducerKafka deleteSyncProducerKafka;

    public SyncPageService(EntityProducerKafka entityProducerKafka, FullSyncProducerKafka fullSyncProducerKafka, DeltaSyncProducerKafka deltaSyncProducerKafka, DeleteSyncProducerKafka deleteSyncProducerKafka) {
        this.entityProducerKafka = entityProducerKafka;
        this.fullSyncProducerKafka = fullSyncProducerKafka;
        this.deltaSyncProducerKafka = deltaSyncProducerKafka;
        this.deleteSyncProducerKafka = deleteSyncProducerKafka;
    }


    public <T extends SyncPage<Object>> void doSync(T syncPage, String domain, String packageName, String entity) {
        Instant start = Instant.now();
        SyncType syncType = syncPage.getSyncType();

        switch (syncType) {
            case FULL -> fullSyncProducerKafka.sendAndGet(syncPage.getMetadata());
            case DELTA -> deltaSyncProducerKafka.sendAndGet(syncPage.getMetadata());
            case DELETE -> {
                syncPage.getResources().forEach(syncPageEntry -> syncPageEntry.setResource(null));
                deleteSyncProducerKafka.send(syncPage.getMetadata());
            }
        }

        sendEntities(syncPage, domain, packageName, entity);
        Instant finish = Instant.now();
        Duration timeElapsed = Duration.between(start, finish);

        logDuration(syncType, syncPage.getMetadata().getCorrId(), timeElapsed);
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

    private void logDuration(SyncType syncType, String corrId, Duration timeTaken) {
        log.info("End {} sync ({}). It took {} hours, {} minutes, {} seconds to complete",
                syncType.toString().toLowerCase(),
                corrId,
                timeTaken.toHoursPart(),
                timeTaken.toMinutesPart(),
                timeTaken.toSecondsPart()
        );
    }

}
