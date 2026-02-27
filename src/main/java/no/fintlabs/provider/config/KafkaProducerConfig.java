package no.fintlabs.provider.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaProducerProperties.class)
public class KafkaProducerConfig {

    private final KafkaProducerProperties kafkaProducerProperties;
    private final ProducerConfig baseProducerConfig;

    public KafkaProducerConfig(
            KafkaProducerProperties kafkaProducerProperties,
            @Qualifier("producerConfig") ProducerConfig baseProducerConfig
    ) {
        this.kafkaProducerProperties = kafkaProducerProperties;
        this.baseProducerConfig = baseProducerConfig;
    }

    @Bean
    @Primary
    public ProducerConfig tunedProducerConfig() {
        Map<String, Object> properties = new HashMap<>(baseProducerConfig.originals());
        if (kafkaProducerProperties.getLinger() != null) {
            properties.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProducerProperties.getLinger().toMillis());
        }
        if (kafkaProducerProperties.getBatchSize() != null) {
            long batchSizeBytes = kafkaProducerProperties.getBatchSize().toBytes();
            if (batchSizeBytes > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("novari.kafka.producer.batch-size exceeds Integer.MAX_VALUE bytes");
            }
            properties.put(ProducerConfig.BATCH_SIZE_CONFIG, (int) batchSizeBytes);
        }
        return new ProducerConfig(properties);
    }

}
