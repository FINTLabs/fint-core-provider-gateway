package no.fintlabs.provider.datasync

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.provider.kafka.TopicNamesConstants.LAST_UPDATED
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_CORRELATION_ID
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_TOTAL_SIZE
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_TYPE
import no.novari.kafka.producing.ParameterizedProducerRecord
import no.novari.kafka.producing.ParameterizedTemplateFactory
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.apache.kafka.common.header.internals.RecordHeaders
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.time.Clock
import java.util.concurrent.CompletableFuture

@Component
class EntityProducer(
    parameterizedTemplateFactory: ParameterizedTemplateFactory,
    private val clock: Clock
) {

    private val producer = parameterizedTemplateFactory.createTemplate(Any::class.java)

    fun sendSyncEntity(syncPage: SyncPage, syncEntry: SyncPageEntry): CompletableFuture<SendResult<String, Any>> =
        syncPage.metadata.toTopic().let { topic ->
            producer.send(
                ParameterizedProducerRecord.builder<Any>()
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
                ParameterizedProducerRecord.builder<Any>()
                    .key(syncPageEntry.identifier)
                    .topicNameParameters(topic)
                    .headers(attachDefaultHeaders(lastUpdated)) // not sync
                    .value(syncPageEntry.resource)
                    .build()
            )
        }

    private fun SyncPageMetadata.toTopic() =
        EntityTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters
                    .stepBuilder()
                    .orgId(this.orgId.topicFormat())
                    .domainContextApplicationDefault()
                    .build()
            )
            .resourceName(uriRef.toComponentPattern())
            .build()

    private fun RequestFintEvent.toTopic(): EntityTopicNameParameters =
        EntityTopicNameParameters.builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters
                    .stepBuilder()
                    .orgId(orgId.topicFormat())
                    .domainContextApplicationDefault()
                    .build()
            )
            .resourceName("$domainName-$packageName-$resourceName")
            .build()

    private fun String.toComponentPattern() =
        this.split("/")
            .take(2)
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
