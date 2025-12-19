package no.fintlabs.provider.datasync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.sync.SyncPage;
import no.fintlabs.adapter.models.sync.SyncPageMetadata;
import no.fintlabs.adapter.models.sync.SyncType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

import static no.fintlabs.provider.kafka.TopicNamesConstants.FINTLABS_NO;

@RequiredArgsConstructor
@Slf4j
@Service
public class SyncPageService {

    private final EntityProducer entityProducer;
    private final MetaDataKafkaProducer metaDataKafkaProducer;

    public <T extends SyncPage> void doSync(T syncPage, String domain, String packageName, String entity) {
        Instant start = logSyncStart(syncPage.getSyncType(), syncPage.getMetadata(), syncPage.getResources().size());

        if (syncPage.getSyncType().equals(SyncType.DELETE)) {
            syncPage.getResources().forEach(syncPageEntry -> syncPageEntry.setResource(null));
        }

        mutateMetadata(syncPage.getMetadata(), domain, packageName, entity);
        String eventName = "adapter-%s-sync".formatted(syncPage.getSyncType().toString().toLowerCase());
        metaDataKafkaProducer.send(syncPage.getMetadata(), FINTLABS_NO, eventName);
        sendEntities(syncPage);

        logSyncEnd(syncPage.getSyncType(), syncPage.getMetadata().getCorrId(), Duration.between(start, Instant.now()));
    }

    private void mutateMetadata(SyncPageMetadata syncPageMetadata, String domain, String packageName, String resourceName) {
        syncPageMetadata.setTime(System.currentTimeMillis());
        syncPageMetadata.setUriRef("%s/%s/%s".formatted(domain.toLowerCase(), packageName.toLowerCase(), resourceName.toLowerCase()));
    }

    private void sendEntities(SyncPage page) {
        page.getResources().forEach(syncPageEntry -> {
            entityProducer.sendSyncEntity(page, syncPageEntry).whenComplete((result, error) -> {
                if (result != null) {
                    log.debug("Entity sent successfully");
                } else {
                    log.error("Error sending entity: " + error.getMessage(), error);
                }
            });
        });
    }

    private Instant logSyncStart(SyncType syncType, SyncPageMetadata metadata, int resourceSize) {
        log.info("Start {} sync: {}({}), {}, total size: {}, page size: {}, page: {}, total pages: {}",
                syncType.toString().toLowerCase(),
                metadata.getCorrId(),
                metadata.getOrgId(),
                metadata.getUriRef(),
                metadata.getTotalSize(),
                resourceSize,
                metadata.getPage(),
                metadata.getTotalPages()
        );

        return Instant.now();
    }

    private void logSyncEnd(SyncType syncType, String corrId, Duration timeTaken) {
        log.info("End {} sync ({}). It took {} hours, {} minutes, {} seconds to complete",
                syncType.toString().toLowerCase(),
                corrId,
                timeTaken.toHoursPart(),
                timeTaken.toMinutesPart(),
                timeTaken.toSecondsPart()
        );
    }

}
