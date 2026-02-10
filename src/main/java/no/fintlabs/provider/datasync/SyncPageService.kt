package no.fintlabs.provider.datasync

import lombok.RequiredArgsConstructor
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.adapter.models.sync.SyncType
import no.fintlabs.provider.kafka.TopicNamesConstants
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@RequiredArgsConstructor
@Service
class SyncPageService(
    private val entityProducer: EntityProducer,
    private val metaDataKafkaProducer: MetaDataKafkaProducer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T : SyncPage> doSync(
        syncPage: T,
        domain: String,
        packageName: String,
        entity: String,
    ) = logSync(syncPage) {
        if (syncPage.syncType == SyncType.DELETE) {
            syncPage.resources.forEach { syncPageEntry -> syncPageEntry.resource = null }
        }

        mutateMetadata(syncPage.metadata, domain, packageName, entity)
        val syncType = syncPage.syncType.toString().lowercase()
        val eventName = "adapter-$syncType-sync"
        metaDataKafkaProducer.send(syncPage.metadata, TopicNamesConstants.FINTLABS_NO, eventName)
        sendEntities(syncPage)
    }

    private fun mutateMetadata(
        syncPageMetadata: SyncPageMetadata,
        domain: String,
        packageName: String,
        resourceName: String,
    ) {
        syncPageMetadata.time = System.currentTimeMillis()
        syncPageMetadata.uriRef = domain.lowercase() + '/' + packageName.lowercase() + '/' + resourceName.lowercase()
    }

    private fun sendEntities(page: SyncPage) {
        page.resources.forEach { syncPageEntry ->
            entityProducer.sendSyncEntity(page, syncPageEntry).whenComplete { result, error ->
                if (result != null) {
                    log.debug("Entity sent successfully")
                } else {
                    log.error("Error sending entity: " + error.message, error)
                }
            }
        }
    }

    private inline fun logSync(
        syncPage: SyncPage,
        action: () -> Unit,
    ) = with(syncPage) {
        log.info(
            "Start {} sync: {}({}), {}, total size: {}, page size: {}, page: {}, total pages: {}",
            syncType.toString().lowercase(),
            metadata.corrId,
            metadata.orgId,
            metadata.uriRef,
            metadata.totalSize,
            resources.size,
            metadata.page,
            metadata.totalPages,
        )

        val time = Instant.now()
        action()
        val timeTaken = Duration.between(time, Instant.now())

        log.info(
            "End {} sync ({}). It took {} minutes, {} seconds, {} milliseconds to complete",
            syncType.toString().lowercase(),
            metadata.corrId,
            timeTaken.toMinutes(),
            timeTaken.toSecondsPart(),
            timeTaken.toMillisPart(),
        )
    }
}
