package no.fintlabs.datasync;

import no.fintlabs.adapter.models.SyncPageEntry;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EntityProducerKafka {

    private final EntityProducer<Object> entityProducer;

    public EntityProducerKafka(EntityProducerFactory entityProducerFactory) {
        this.entityProducer = entityProducerFactory.createProducer(Object.class);
    }

    public CompletableFuture<SendResult<String, Object>> sendEntity(String orgId, String domain, String packageName, String entityName, SyncPageEntry<Object> syncPageEntry) {
        return sendEntity(orgId, domain, packageName, entityName, syncPageEntry, null);
    }

    public CompletableFuture<SendResult<String, Object>> sendEntity(String orgId, String domain, String packageName, String entityName, SyncPageEntry<Object> syncPageEntry, String eventCorrId) {

        RecordHeaders headers = new RecordHeaders();
        if (StringUtils.isNotBlank(eventCorrId)) headers.add(new RecordHeader("event-corr-id", eventCorrId.getBytes()));

        return entityProducer.send(
                EntityProducerRecord.builder()
                        .topicNameParameters(EntityTopicNameParameters
                                .builder()
                                .orgId(orgId)
                                .resource("%s-%s-%s".formatted(domain, packageName, entityName))
                                .build())
                        .headers(headers)
                        .key(syncPageEntry.getIdentifier())
                        .value(syncPageEntry.getResource())
                        .build()
        );
    }
}
