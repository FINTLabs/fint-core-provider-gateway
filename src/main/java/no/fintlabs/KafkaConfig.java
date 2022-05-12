package no.fintlabs;

import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.adapter.models.AdapterPing;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.event.EventProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

//    @Bean
//    public EntityProducer<Object> entityProducer(FintKafkaEntityProducerFactory entityProducerFactory) {
//        return entityProducerFactory.createProducer(Object.class);
//    }
//
//    @Bean
//    EventProducer<AdapterContract> contractEventProducer(FintKafkaEventProducerFactory eventProducerFactory) {
//        return eventProducerFactory.createProducer(AdapterContract.class);
//    }
//
//    @Bean
//    EventProducer<AdapterPing> pingEventProducer(FintKafkaEventProducerFactory eventProducerFactory) {
//        return eventProducerFactory.createProducer(AdapterPing.class);
//    }
}
