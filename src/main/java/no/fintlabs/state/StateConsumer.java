package no.fintlabs.state;

import no.fintlabs.adapter.models.SyncPageMetadata;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;

import javax.annotation.PostConstruct;
import java.util.function.Consumer;

public class StateConsumer {

    private final EventConsumerFactoryService consumerFactory;
    private final StateRepository stateRepository;

    public StateConsumer(EventConsumerFactoryService consumerFactory, StateRepository stateRepository) {
        this.consumerFactory = consumerFactory;
        this.stateRepository = stateRepository;
    }


    @PostConstruct
    public void init() {
        //Pattern.compile(".*.fint-core\\.event\\.adapter-full-sync")
        consumerFactory.createFactory(
                SyncPageMetadata.class,
                onAdapterPing(),
                new CommonLoggingErrorHandler(),
                true
        ).createContainer(
                EventTopicNamePatternParameters
                        .builder()
                        .orgId(FormattedTopicComponentPattern.any())
                        .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                        .eventName(ValidatedTopicComponentPattern.anyOf("adapter-full-sync"))
                        .build()
        );

    }

    private Consumer<ConsumerRecord<String, SyncPageMetadata>> onAdapterPing() {
        return (ConsumerRecord<String, SyncPageMetadata> record) -> {
            SyncPageMetadata metadata = record.value();
            if (stateRepository.has(metadata.getCorrId())) {
                stateRepository.update(metadata);
            }
            stateRepository.add(metadata);
        };
    }
}
