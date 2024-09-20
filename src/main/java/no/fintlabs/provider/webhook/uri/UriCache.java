package no.fintlabs.provider.webhook.uri;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UriCache {

    private final static String DOMAIN_URI_PATTERN = "/%s";
    private final static String COMPONENT_URI_PATTERN = "/%s/%s";
    private final static String RESOURCE_URI_PATTERN = "/%s/%s/%s";

    private final WebClient webhookWebClient;
    private final Set<String> validDomainUris = new HashSet<>();
    private final Set<String> validComponentUris = new HashSet<>();
    private final Set<String> validResourceUris = new HashSet<>();

    public boolean uriIsValid(String uri) {
        return validDomainUris.contains(uri) || validComponentUris.contains(uri) || validResourceUris.contains(uri);
    }

    @PostConstruct
    private void fillCaches() {
        webhookWebClient.get()
                .retrieve()
                .bodyToFlux(Metadata.class)
                .subscribe(metadata -> {
                    validDomainUris.add(DOMAIN_URI_PATTERN.formatted(metadata.domainName()));
                    validComponentUris.add(COMPONENT_URI_PATTERN.formatted(metadata.domainName(), metadata.packageName()));
                    validResourceUris.add(RESOURCE_URI_PATTERN.formatted(metadata.domainName(), metadata.packageName(), metadata.resourceName()));
                });
    }

}
