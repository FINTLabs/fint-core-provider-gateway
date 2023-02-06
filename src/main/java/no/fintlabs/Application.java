package no.fintlabs;

import no.fintlabs.adapter.models.SyncPageMetadata;
import no.fintlabs.kafka.KafkaEventProducer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
