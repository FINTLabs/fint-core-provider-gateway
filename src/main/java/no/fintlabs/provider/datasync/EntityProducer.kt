package no.fintlabs.provider.datasync

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.kafka.common.topic.TopicNameParameters
import no.fintlabs.kafka.entity.EntityProducerFactory
import no.fintlabs.kafka.entity.EntityProducerRecord
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters
import no.fintlabs.provider.kafka.ProviderTopicService
import no.fintlabs.provider.kafka.TopicNamesConstants.*
import org.apache.kafka.common.header.internals.RecordHeaders
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.time.Clock
import java.util.concurrent.CompletableFuture

@Component
class EntityProducer(
    entityProducerFactory: EntityProducerFactory,
    private val topicService: ProviderTopicService,
    private val clock: Clock
) {

    private val producer = entityProducerFactory.createProducer(Any::class.java)

    fun sendSyncEntity(syncPage: SyncPage, syncEntry: SyncPageEntry): CompletableFuture<SendResult<String, Any>> =
        syncPage.metadata.toTopic().let { topic ->
            producer.send(
                EntityProducerRecord.builder<Any>()
                    .key(syncEntry.identifier)
                    .topicNameParameters(topic)
                    .headers(attachSyncHeaders(topic, syncPage))
                    .value(syncEntry.resource)
                    .build()
            )
        }

    fun sendEventEntity(
        request: RequestFintEvent,
        syncPageEntry: SyncPageEntry
    ): CompletableFuture<SendResult<String, Any>> =
        request.toTopic().let { topic ->
            producer.send(
                EntityProducerRecord.builder<Any>()
                    .key(syncPageEntry.identifier)
                    .topicNameParameters(topic)
                    .headers(attachDefaultHeaders(topic)) // not sync
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
            .orgId(orgId.replace("-", "."))
            .domainContext(FINT_CORE)
            .resource("$domainName-$packageName-$resourceName")
            .build()

    private fun String.toTopicResource() =
        this.split("/")
            .take(3)
            .joinToString("-")


    private fun String.topicFormat() = this.replace(".", "-")

    private fun attachDefaultHeaders(topic: TopicNameParameters) =
        RecordHeaders().apply {
            add(LAST_MODIEFIED, clock.millis().toByteArray())
            attachTopicRetentionIfValid(this, topic)
        }

    private fun attachSyncHeaders(topic: TopicNameParameters, syncPage: SyncPage) =
        attachDefaultHeaders(topic).apply {
            add(SYNC_TYPE, byteArrayOf(syncPage.syncType.ordinal.toByte()))
            add(SYNC_CORRELATION_ID, syncPage.metadata.corrId.toByteArray())
            add(SYNC_TOTAL_SIZE, syncPage.metadata.totalSize.toByteArray())
        }

    private fun attachTopicRetentionIfValid(records: RecordHeaders, topic: TopicNameParameters) =
        topicService.getRetensionTime(topic).takeIf { it.validRetentionTime() }
            ?.let { records.add(TOPIC_RETENTION_TIME, it.toByteArray()) }

    private fun Long.validRetentionTime() = this != 0L

    private fun Long.toByteArray(): ByteArray =
        ByteBuffer.allocate(Long.SIZE_BYTES)
            .putLong(this)
            .array()

}