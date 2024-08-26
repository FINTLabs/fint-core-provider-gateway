package no.fintlabs.provider.datasync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.SyncPage;
import no.fintlabs.adapter.models.SyncPageMetadata;
import no.fintlabs.adapter.models.SyncType;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Slf4j
@Service
public class SyncPageService {

    private final EntityProducerKafka entityProducerKafka;
    private final MetaDataKafkaProducer metaDataKafkaProducer;

    public <T extends SyncPage<Object>> void doSync(T syncPage, String domain, String packageName, String entity) {
        Instant start = Instant.now();
        logSyncStart(syncPage.getSyncType(), syncPage.getMetadata(), syncPage.getResources().size());

        if (syncPage.getSyncType().equals(SyncType.DELETE)) {
            syncPage.getResources().forEach(syncPageEntry -> syncPageEntry.setResource(null));
        }

        String eventName = "adapter-%s-sync".formatted(syncPage.getSyncType().toString().toLowerCase());
        metaDataKafkaProducer.send(syncPage.getMetadata(), syncPage.getMetadata().getOrgId(), eventName);
        sendEntities(syncPage, domain, packageName, entity);

        logSyncEnd(syncPage.getSyncType(), syncPage.getMetadata().getCorrId(), Duration.between(start, Instant.now()));
    }

    private void sendEntities(SyncPage<Object> page, String domain, String packageName, String entity) {
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters.builder()
                .orgId(page.getMetadata().getOrgId())
                .resource("%s-%s-%s".formatted(domain, packageName, entity))
                .build();

        page.getResources().forEach(syncPageEntry -> {
            CompletableFuture<SendResult<String, Object>> future = entityProducerKafka.sendEntity(entityTopicNameParameters, syncPageEntry);

            future.whenComplete((result, error) -> {
                if (result != null) {
                    log.debug("Entity sent successfully");
                } else {
                    log.error("Error sending entity: " + error.getMessage(), error);
                }
            });
        });
    }

    private static void logSyncStart(SyncType syncType, SyncPageMetadata metadata, int resourceSize) {
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
