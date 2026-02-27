package no.fintlabs.provider.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "novari.kafka.producer")
public class KafkaProducerProperties {

    private Duration linger;
    private DataSize batchSize;

}
