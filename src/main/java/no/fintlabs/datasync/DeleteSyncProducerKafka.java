package no.fintlabs.datasync;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.SyncPageMetadata;
import no.fintlabs.kafka.EventProducerKafka;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Slf4j
@Service
public class DeleteSyncProducerKafka extends EventProducerKafka<SyncPageMetadata> {

    public DeleteSyncProducerKafka(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, SyncPageMetadata.class, "adapter-delete-sync");
    }

    public ListenableFuture send(SyncPageMetadata syncPageMetadata) {
        return super.send(syncPageMetadata, syncPageMetadata.getOrgId());
    }

}
