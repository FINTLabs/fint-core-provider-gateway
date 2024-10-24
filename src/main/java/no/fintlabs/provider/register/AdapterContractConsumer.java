package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.provider.config.KafkaConfig;
import no.fintlabs.provider.security.AdapterContractContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Service;

import static no.fintlabs.provider.kafka.TopicNamesConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdapterContractConsumer {

    private final AdapterContractContext adapterContractContext;
    private final EventConsumerFactoryService eventConsumerFactoryService;
    private final AdapterRegistrationTopicService adapterRegistrationTopicService;
    private final KafkaConfig kafkaConfig;

    @Bean
    public ConcurrentMessageListenerContainer<String, AdapterContract> registerAdapterContractListener() {
        return eventConsumerFactoryService.createFactory(
                AdapterContract.class,
                this::processEvent,
                EventConsumerConfiguration
                        .builder()
                        .seekingOffsetResetOnAssignment(true)
                        .groupIdSuffix(kafkaConfig.getGroupIdSuffix())
                        .build()
        ).createContainer(
                EventTopicNameParameters
                        .builder()
                        .orgId(FINTLABS_NO)
                        .domainContext(FINT_CORE)
                        .eventName(ADAPTER_REGISTER_EVENT_NAME)
                        .build()
        );
    }

    private void processEvent(ConsumerRecord<String, AdapterContract> consumerRecord) {
        adapterRegistrationTopicService.ensureCapabilityTopics(consumerRecord.value());
        adapterContractContext.add(consumerRecord.value());
    }

}
