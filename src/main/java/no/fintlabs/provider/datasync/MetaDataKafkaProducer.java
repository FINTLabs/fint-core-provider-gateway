package no.fintlabs.provider.datasync;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.sync.SyncPageMetadata;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.topic.EventTopicService;
import no.fintlabs.provider.kafka.EventProducerKafka;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetaDataKafkaProducer extends EventProducerKafka<SyncPageMetadata> {
    public MetaDataKafkaProducer(EventProducerFactory eventProducerFactory, EventTopicService eventTopicService) {
        super(eventProducerFactory, eventTopicService, SyncPageMetadata.class);
    }
}
