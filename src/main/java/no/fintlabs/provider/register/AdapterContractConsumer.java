package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.kafka.common.topic.pattern.FormattedTopicComponentPattern;
import no.fintlabs.kafka.common.topic.pattern.ValidatedTopicComponentPattern;
import no.fintlabs.kafka.event.EventConsumerConfiguration;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNamePatternParameters;
import no.fintlabs.provider.config.KafkaConfig;
import no.fintlabs.provider.security.AdapterContractContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Service;

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
                EventTopicNamePatternParameters
                        .builder()
                        .orgId(FormattedTopicComponentPattern.any())
                        .domainContext(FormattedTopicComponentPattern.anyOf("fint-core"))
                        .eventName(ValidatedTopicComponentPattern.endingWith("adapter-register"))
                        .build()
        );
    }

    private void processEvent(ConsumerRecord<String, AdapterContract> consumerRecord) {
        log.info("Adapter Contract consumed: {} - {}", consumerRecord.value().getOrgId(), consumerRecord.value().getAdapterId());
        adapterRegistrationTopicService.ensureTopics(consumerRecord.value());
        adapterContractContext.add(consumerRecord.value().getAdapterId(), consumerRecord.value().getUsername());
    }

}
