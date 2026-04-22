package no.fintlabs.provider.datasync

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.LAST_UPDATED
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.RESOURCE_NAME
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.SYNC_CORRELATION_ID
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.SYNC_TOTAL_SIZE
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.SYNC_TYPE
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

    companion object {
        const val KEY_DELIMITER = "\u001F"
    }

    fun sendSyncEntity(
        syncPage: SyncPage,
        syncEntry: SyncPageEntry
    ): CompletableFuture<SendResult<String, Any>> =
        syncPage.metadata.toTopic().let { topic ->
            producer.send(
                ParameterizedProducerRecord.builder<Any>()
                    .key("${syncPage.getResourceName()}$KEY_DELIMITER${syncEntry.identifier}")
                    .topicNameParameters(topic)
                    .headers(attachSyncHeaders(syncPage))
                    .value(syncEntry.resource)
                    .build()
            )
        }

    fun sendEventEntity(
        request: RequestFintEvent,
        syncEntry: SyncPageEntry,
        lastUpdated: Long
    ): CompletableFuture<SendResult<String, Any>> =
        request.toTopic().let { topic ->
            producer.send(
                ParameterizedProducerRecord.builder<Any>()
                    .key("${request.resourceName}$KEY_DELIMITER${syncEntry.identifier}")
                    .topicNameParameters(topic)
                    .headers(attachDefaultHeaders(request.resourceName, lastUpdated)) // not sync
                    .value(syncEntry.resource)
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
            .resourceName("$domainName-$packageName")
            .build()

    private fun String.toComponentPattern() =
        this.split("/")
            .take(2)
            .joinToString("-")

    private fun String.topicFormat() = this.replace(".", "-")

    private fun attachDefaultHeaders(resourceName: String, lastUpdated: Long = clock.millis()) =
        RecordHeaders().apply {
            add(RESOURCE_NAME, resourceName.toByteArray())
            add(LAST_UPDATED, lastUpdated.toByteArray())
        }

    private fun attachSyncHeaders(syncPage: SyncPage) =
        attachDefaultHeaders(syncPage.getResourceName()).apply {
            add(SYNC_TYPE, byteArrayOf(syncPage.syncType.ordinal.toByte()))
            add(SYNC_CORRELATION_ID, syncPage.metadata.corrId.toByteArray())
            add(SYNC_TOTAL_SIZE, syncPage.metadata.totalSize.toByteArray())
        }

    private fun SyncPage.getResourceName() = metadata.uriRef.split("/").last()

    private fun Long.toByteArray(): ByteArray =
        ByteBuffer.allocate(Long.SIZE_BYTES)
            .putLong(this)
            .array()

}
