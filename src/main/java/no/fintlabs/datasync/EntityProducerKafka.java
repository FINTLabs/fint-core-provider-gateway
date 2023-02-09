package no.fintlabs.datasync;

import no.fintlabs.adapter.models.SyncPageEntry;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class EntityProducerKafka {

    private final EntityProducer<Object> entityProducer;

    public EntityProducerKafka(EntityProducerFactory entityProducerFactory) {
        this.entityProducer = entityProducerFactory.createProducer(Object.class);
    }

    public ListenableFuture<SendResult<String, Object>> sendEntity(String orgId, String domain, String packageName, String entityName, SyncPageEntry<Object> entity) {
        return entityProducer.send(
                EntityProducerRecord.builder()
                        .topicNameParameters(EntityTopicNameParameters
                                .builder()
                                .orgId(orgId)
                                .resource(String.format("%s-%s-%s", domain, packageName, entityName))
                                .build())
                        .key(entity.getIdentifier())
                        .value(entity.getResource())
                        .build()
        );
    }
}
