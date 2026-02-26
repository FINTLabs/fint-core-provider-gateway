package no.fintlabs.provider.datasync

import java.nio.ByteBuffer
import java.time.Clock
import java.time.Duration
import java.util.concurrent.CompletableFuture
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.sync.SyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.provider.kafka.ProviderTopicService
import no.fintlabs.provider.kafka.TopicNamesConstants.LAST_MODIEFIED
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_CORRELATION_ID
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_TOTAL_SIZE
import no.fintlabs.provider.kafka.TopicNamesConstants.SYNC_TYPE
import no.fintlabs.provider.kafka.TopicNamesConstants.TOPIC_RETENTION_TIME
import no.novari.kafka.producing.ParameterizedProducerRecord
import no.novari.kafka.producing.ParameterizedTemplateFactory
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.apache.kafka.common.header.internals.RecordHeaders
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component

@Component
class EntityProducer(
    parameterizedTemplateFactory: ParameterizedTemplateFactory,
    private val topicService: ProviderTopicService,
    private val clock: Clock
) {

    private val producer = parameterizedTemplateFactory.createTemplate(Any::class.java)

    fun sendSyncEntity(syncPage: SyncPage, syncEntry: SyncPageEntry): CompletableFuture<SendResult<String, Any>> =
        syncPage.metadata.toTopic().let { topic ->
            producer.send(
                ParameterizedProducerRecord.builder<Any>()
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
                ParameterizedProducerRecord.builder<Any>()
                    .key(syncPageEntry.identifier)
                    .topicNameParameters(topic)
                    .headers(attachDefaultHeaders(topic)) // not sync
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
            .resourceName(uriRef.toTopicResource())
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
        topicService.getRetentionTime(topic).takeIf { it.validRetentionTime() }
            ?.let { records.add(TOPIC_RETENTION_TIME, it.toByteArray()) }

    private fun Duration.validRetentionTime() = !this.isZero

    private fun Long.toByteArray(): ByteArray =
        ByteBuffer.allocate(Long.SIZE_BYTES)
            .putLong(this)
            .array()

    private fun Duration.toByteArray(): ByteArray =
        ByteBuffer.allocate(Long.SIZE_BYTES)
            .putLong(this.toMillis())
            .array()

}
