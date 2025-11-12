package no.fintlabs.provider.datasync;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.sync.SyncPageEntry;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.provider.kafka.ProviderTopicService;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static no.fintlabs.provider.kafka.TopicNamesConstants.LAST_MODIEFIED;
import static no.fintlabs.provider.kafka.TopicNamesConstants.TOPIC_RETENTION_TIME;

@Slf4j
@Service
public class EntityProducerKafka {

    private final ProviderTopicService topicService;
    private final EntityProducer<Object> entityProducer;

    public EntityProducerKafka(ProviderTopicService topicService, EntityProducerFactory entityProducerFactory) {
        this.topicService = topicService;
        this.entityProducer = entityProducerFactory.createProducer(Object.class);
    }

    public CompletableFuture<SendResult<String, Object>> sendEntity(EntityTopicNameParameters entityTopicName, SyncPageEntry syncPageEntry) {
        return sendEntity(entityTopicName, syncPageEntry, null);
    }

    public CompletableFuture<SendResult<String, Object>> sendEntity(EntityTopicNameParameters entityTopicName, SyncPageEntry syncPageEntry, String eventCorrId) {
        return entityProducer.send(
                EntityProducerRecord.builder()
                        .topicNameParameters(entityTopicName)
                        .headers(createHeaders(entityTopicName, eventCorrId))
                        .key(syncPageEntry.getIdentifier())
                        .value(syncPageEntry.getResource())
                        .build()
        );
    }

    private RecordHeaders createHeaders(EntityTopicNameParameters entityTopicName, String eventCorrId) {
        RecordHeaders headers = new RecordHeaders();
        attachLastModified(headers);
        attachTopicRetentionTime(headers, entityTopicName);
        attachEventCorrId(headers, eventCorrId);
        return headers;
    }

    private void attachEventCorrId(RecordHeaders headers, String eventCorrId) {
        if (StringUtils.isNotBlank(eventCorrId)) headers.add("event-corr-id", eventCorrId.getBytes());
    }

    private void attachLastModified(RecordHeaders headers) {
        headers.add(
                LAST_MODIEFIED,
                ByteBuffer.allocate(Long.BYTES).putLong(System.currentTimeMillis()).array()
        );
    }

    private void attachTopicRetentionTime(RecordHeaders headers, EntityTopicNameParameters entityTopicName) {
        long topicRetentionTime = topicService.getRetensionTime(entityTopicName);
        if (topicRetentionTime != 0L) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(topicRetentionTime);
            headers.add(TOPIC_RETENTION_TIME, buffer.array());
        }
    }
}
