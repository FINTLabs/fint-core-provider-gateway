package no.fintlabs.provider.datasync

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.kafka.entity.EntityProducerFactory
import no.fintlabs.kafka.entity.EntityProducerRecord
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import no.fintlabs.provider.kafka.TopicNamesConstants.FINT_CORE
import no.fintlabs.provider.kafka.TopicNamesConstants.LAST_UPDATED
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_CORRELATION_ID
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_TOTAL_SIZE
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_TYPE
import org.apache.kafka.common.header.internals.RecordHeaders
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.time.Clock
import java.util.concurrent.CompletableFuture

@Component
class EntityProducer(
    entityProducerFactory: EntityProducerFactory,
    private val clock: Clock
) {

    private val producer = entityProducerFactory.createProducer(Any::class.java)

    fun sendSyncEntity(syncPage: SyncPage, syncEntry: SyncPageEntry): CompletableFuture<SendResult<String, Any>> =
        syncPage.metadata.toTopic().let { topic ->
            producer.send(
                EntityProducerRecord.builder<Any>()
                    .key(syncEntry.identifier)
                    .topicNameParameters(topic)
                    .headers(attachSyncHeaders(syncPage))
                    .value(syncEntry.resource)
                    .build()
            )
        }

    fun sendEventEntity(
        request: RequestFintEvent,
        syncPageEntry: SyncPageEntry,
        lastUpdated: Long
    ): CompletableFuture<SendResult<String, Any>> =
        request.toTopic().let { topic ->
            producer.send(
                EntityProducerRecord.builder<Any>()
                    .key(syncPageEntry.identifier)
                    .topicNameParameters(topic)
                    .headers(attachDefaultHeaders(lastUpdated)) // not sync
                    .value(syncPageEntry.resource)
                    .build()
            )
        }

    private fun SyncPageMetadata.toTopic() =
        EntityTopicNameParameters.builder()
            .orgId(this.orgId.topicFormat())
            .domainContext(FINT_CORE)
            .resource(uriRef.toTopicResource())
            .build()

    private fun RequestFintEvent.toTopic(): EntityTopicNameParameters =
        EntityTopicNameParameters.builder()
            .orgId(orgId.topicFormat())
            .domainContext(FINT_CORE)
            .resource("$domainName-$packageName-$resourceName")
            .build()

    private fun String.toTopicResource() =
        this.split("/")
            .take(3)
            .joinToString("-")

    private fun String.topicFormat() = this.replace(".", "-")

    private fun attachDefaultHeaders(lastUpdated: Long = clock.millis()) =
        RecordHeaders().apply { add(LAST_UPDATED, lastUpdated.toByteArray()) }

    private fun attachSyncHeaders(syncPage: SyncPage) =
        attachDefaultHeaders().apply {
            add(SYNC_TYPE, byteArrayOf(syncPage.syncType.ordinal.toByte()))
            add(SYNC_CORRELATION_ID, syncPage.metadata.corrId.toByteArray())
            add(SYNC_TOTAL_SIZE, syncPage.metadata.totalSize.toByteArray())
        }

    private fun Long.toByteArray(): ByteArray =
        ByteBuffer.allocate(Long.SIZE_BYTES)
            .putLong(this)
            .array()

}