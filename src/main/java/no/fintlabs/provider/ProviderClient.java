package no.fintlabs.provider;

import lombok.RequiredArgsConstructor;
import no.fintlabs.provider.kafka.offset.OffsetState;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ProviderClient {

    private final RestClient providerRestClient;

    public OffsetState getExistingOffsetState() {
        return providerRestClient.get()
                .uri("/provider/offset")
                .retrieve()
                .body(OffsetState.class);
    }

}
