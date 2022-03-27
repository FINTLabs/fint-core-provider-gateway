package no.fintlabs.state;

import no.fintlabs.adapter.models.FullSyncPage;
import no.fintlabs.kafka.event.FintKafkaEventConsumerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;

import javax.annotation.PostConstruct;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class StateConsumer {

    private final FintKafkaEventConsumerFactory consumerFactory;
    private final StateRepository stateRepository;

    public StateConsumer(FintKafkaEventConsumerFactory consumerFactory, StateRepository stateRepository) {
        this.consumerFactory = consumerFactory;
        this.stateRepository = stateRepository;
    }


    @PostConstruct
    public void init() {

        consumerFactory.createConsumerWithResetOffset(
                Pattern.compile(".*.fint-core\\.event\\.adapter-full-sync"),
                FullSyncPage.Metadata.class,
                onAdapterPing(),
                new CommonLoggingErrorHandler()
        );
    }

    private Consumer<ConsumerRecord<String, FullSyncPage.Metadata>> onAdapterPing() {
        return (ConsumerRecord<String, FullSyncPage.Metadata> record) -> {
            FullSyncPage.Metadata metadata = record.value();
            if (stateRepository.has(metadata.getCorrId())) {
                stateRepository.update(metadata);
            }
            stateRepository.add(metadata);
        };
    }
}
