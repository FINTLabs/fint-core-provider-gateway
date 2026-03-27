package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.provider.config.KafkaConfig;
import no.fintlabs.provider.security.AdapterContractContext;
import no.novari.kafka.consuming.ErrorHandlerConfiguration;
import no.novari.kafka.consuming.ErrorHandlerFactory;
import no.novari.kafka.consuming.ListenerConfiguration;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import no.novari.kafka.topic.name.TopicNamePrefixParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.ADAPTER_REGISTER_EVENT_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdapterContractConsumer {

    private final AdapterContractContext adapterContractContext;
    private final ParameterizedListenerContainerFactoryService parameterizedListenerContainerFactoryService;
    private final ErrorHandlerFactory errorHandlerFactory;
    private final KafkaConfig kafkaConfig;

    @Bean
    public ConcurrentMessageListenerContainer<String, AdapterContract> registerAdapterContractListener() {
        return parameterizedListenerContainerFactoryService.createRecordListenerContainerFactory(
                AdapterContract.class,
                this::processEvent,
                ListenerConfiguration
                        .stepBuilder()
                        .groupIdApplicationDefaultWithSuffix(kafkaConfig.getGroupIdSuffix())
                        .maxPollRecordsKafkaDefault()
                        .maxPollIntervalKafkaDefault()
                        .seekToBeginningOnAssignment()
                        .build(),
                errorHandlerFactory.createErrorHandler(
                        ErrorHandlerConfiguration
                                .<AdapterContract>stepBuilder()
                                .noRetries()
                                .skipFailedRecords()
                                .build()
                )
        ).createContainer(
                EventTopicNameParameters
                        .builder()
                        .topicNamePrefixParameters(
                                TopicNamePrefixParameters
                                        .stepBuilder()
                                        .orgIdApplicationDefault()
                                        .domainContextApplicationDefault()
                                        .build()
                        )
                        .eventName(ADAPTER_REGISTER_EVENT_NAME)
                        .build()
        );
    }

    private void processEvent(ConsumerRecord<String, AdapterContract> consumerRecord) {
        adapterContractContext.add(consumerRecord.value());
    }

}
