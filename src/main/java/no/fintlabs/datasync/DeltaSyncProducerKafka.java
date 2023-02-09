package no.fintlabs.datasync;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.SyncPageMetadata;
import no.fintlabs.kafka.EventProducerKafka;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class DeltaSyncProducerKafka extends EventProducerKafka<SyncPageMetadata> {

    public DeltaSyncProducerKafka(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, SyncPageMetadata.class, "adapter-delta-sync");
    }

    public ListenableFuture send(SyncPageMetadata syncPageMetadata) {
        return super.send(syncPageMetadata, syncPageMetadata.getOrgId());
    }

    public void sendAndGet(SyncPageMetadata metadata) {
        try {
            send(metadata).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
        }
    }

}
