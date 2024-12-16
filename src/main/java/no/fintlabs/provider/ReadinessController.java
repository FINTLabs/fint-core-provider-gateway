package no.fintlabs.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.provider.kafka.offset.OffsetState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ready")
public class ReadinessController {

    private final OffsetState offsetState;
    private final ProviderClient providerClient;

    @GetMapping
    public ResponseEntity<Void> readinessCheck() {
        try {
            return offsetState.equals(providerClient.getExistingOffsetState())
                    ? ResponseEntity.ok().build()
                    : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (RestClientResponseException e) {
            log.error("Failed fetching readiness check with status code: {}", e.getStatusCode());
            return ResponseEntity.ok().build();
        }
    }

}
