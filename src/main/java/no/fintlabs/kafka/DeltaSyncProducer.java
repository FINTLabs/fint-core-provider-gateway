package no.fintlabs.kafka;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.SyncPageMetadata;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class DeltaSyncProducer extends KafkaEventProducer<SyncPageMetadata> {

    public DeltaSyncProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, SyncPageMetadata.class, "adapter-delta-sync");
    }

    public void send(SyncPageMetadata syncPageMetadata) {
        super.send(syncPageMetadata, syncPageMetadata.getOrgId());
    }

    public void sendAndGet(SyncPageMetadata metadata) {
        try {
            send(metadata, metadata.getOrgId()).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
        }
    }

}
