package no.fintlabs.provider.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final ProviderProperties providerProperties;

    @Bean("providerRestClient")
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(providerProperties.getPodUrl())
                .build();
    }

}
