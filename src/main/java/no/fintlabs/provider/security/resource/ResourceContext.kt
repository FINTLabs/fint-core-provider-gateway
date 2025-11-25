package no.fintlabs.provider.security.resource;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Component
public class ResourceContext {

    private final Set<String> validResources;

    public ResourceContext(@Value("${fint.metamodel.base-url}") String metamodelBaseUrl) {
        validResources = fetchValidResources(WebClient.builder().baseUrl(metamodelBaseUrl).build());
    }

    private Set<String> fetchValidResources(WebClient webClient) {
        return webClient.get()
                .retrieve()
                .bodyToFlux(ResourceMetadata.class)
                .map(resourceMetadata ->
                        String.format("%s-%s-%s",
                                resourceMetadata.domainName(),
                                resourceMetadata.packageName(),
                                resourceMetadata.resourceName())
                )
                .collect(Collectors.toSet())
                .block();
    }
}
