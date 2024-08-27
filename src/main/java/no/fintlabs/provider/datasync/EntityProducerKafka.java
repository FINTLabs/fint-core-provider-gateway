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

import static no.fintlabs.provider.kafka.TopicNamesConstants.HEADER_RETENSION_TIME;

@Slf4j
@Service
public class EntityProducerKafka {

    private final ProviderTopicService topicService;
    private final EntityProducer<Object> entityProducer;
    private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

    public EntityProducerKafka(ProviderTopicService topicService, EntityProducerFactory entityProducerFactory) {
        this.topicService = topicService;
        this.entityProducer = entityProducerFactory.createProducer(Object.class);
    }

    public CompletableFuture<SendResult<String, Object>> sendEntity(EntityTopicNameParameters entityTopicName, SyncPageEntry syncPageEntry) {
        return sendEntity(entityTopicName, syncPageEntry, null);
    }

    public CompletableFuture<SendResult<String, Object>> sendEntity(EntityTopicNameParameters entityTopicName, SyncPageEntry syncPageEntry, String eventCorrId) {

        RecordHeaders headers = new RecordHeaders();
        byte[] retensionArray = buffer.putLong(topicService.getRetensionTime(entityTopicName)).array();
        log.debug("Attaching retension time: {} to header", retensionArray);
        headers.add(HEADER_RETENSION_TIME, retensionArray);

        if (StringUtils.isNotBlank(eventCorrId)) headers.add("event-corr-id", eventCorrId.getBytes());

        return entityProducer.send(
                EntityProducerRecord.builder()
                        .topicNameParameters(entityTopicName)
                        .headers(headers)
                        .key(syncPageEntry.getIdentifier())
                        .value(syncPageEntry.getResource())
                        .build()
        );
    }
}
