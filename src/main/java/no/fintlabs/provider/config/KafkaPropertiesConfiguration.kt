package no.fintlabs.provider.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KafkaPropertiesConfiguration(private val kafkaProperties: KafkaProperties) {

    @Bean
    fun entityKafkaProperties(): EntityKafkaProperties = kafkaProperties.entity

    @Bean
    fun adapterKafkaProperties(): AdapterKafkaProperties = kafkaProperties.adapter

    @Bean
    fun responseProducerProperties(): ProducerProperties = kafkaProperties.event.responseProducer

    @Bean
    fun responseConsumerProperties(): ConsumerProperties = kafkaProperties.event.responseConsumer

    @Bean
    fun requestConsumerProperties(): ConsumerProperties = kafkaProperties.event.requestConsumer
}