package no.fintlabs.provider.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.event.RequestFintEvent;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static no.fintlabs.provider.webhook.uri.UriConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebClient webClient;
    private final WebhookRegistrationValidator webhookRegistrationValidator;
    private final Map<String, Set<String>> uriToCallBackUriMap = new ConcurrentHashMap<>();

    public void register(WebhookRegistration webhookRegistration) {
        webhookRegistrationValidator.valiateRegistration(webhookRegistration);
        webhookRegistration.listenUris().forEach(listenUri ->
                uriToCallBackUriMap
                        .computeIfAbsent(listenUri, k -> Collections.synchronizedSet(new HashSet<>()))
                        .add(webhookRegistration.callbackUrl())
        );
    }

    public void publishEvent(RequestFintEvent requestFintEvent) {
        Set<String> allCallbackUrls = gatherCallbackUrls(requestFintEvent);
        Set<String> calledCallbackUrls = new HashSet<>();

        publishToAllUrls(requestFintEvent, allCallbackUrls, calledCallbackUrls);
    }

    private Set<String> gatherCallbackUrls(RequestFintEvent requestFintEvent) {
        HashSet<String> allCallbackUrls = new HashSet<>();

        allCallbackUrls.addAll(uriToCallBackUriMap.getOrDefault(
                DOMAIN_URI_PATTERN.formatted(requestFintEvent.getDomainName()), Collections.emptySet()));
        allCallbackUrls.addAll(uriToCallBackUriMap.getOrDefault(
                COMPONENT_URI_PATTERN.formatted(requestFintEvent.getDomainName(), requestFintEvent.getPackageName()), Collections.emptySet()));
        allCallbackUrls.addAll(uriToCallBackUriMap.getOrDefault(
                RESOURCE_URI_PATTERN.formatted(requestFintEvent.getDomainName(), requestFintEvent.getPackageName(), requestFintEvent.getResourceName()), Collections.emptySet()));

        return allCallbackUrls;
    }

    private void publishToAllUrls(RequestFintEvent requestFintEvent, Set<String> callbackUrls, Set<String> calledCallbackUrls) {
        callbackUrls.stream()
                .filter(calledCallbackUrls::add)
                .forEach(callbackUrl -> sendEventToCallbackUrl(requestFintEvent, callbackUrl));
    }

    private void sendEventToCallbackUrl(RequestFintEvent requestFintEvent, String callbackUrl) {
        webClient.post()
                .uri(callbackUrl)
                .bodyValue(requestFintEvent)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::logError)
                .toBodilessEntity()
                .subscribe();
    }

    private Mono<? extends Throwable> logError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class).flatMap(responseBody -> {
            log.error("Failed to callback url: {} with status code: {} and body: {}",
                    clientResponse.request().getURI(), clientResponse.statusCode(), responseBody);
            return Mono.empty();
        });
    }
}
