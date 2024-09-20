package no.fintlabs.provider.webhook;

import lombok.RequiredArgsConstructor;
import no.fintlabs.provider.webhook.exception.InvalidListeningUriException;
import no.fintlabs.provider.webhook.exception.InvalidUrlException;
import no.fintlabs.provider.webhook.uri.UriCache;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WebhookRegistrationValidator {

    private final UriCache uriCache;

    public void valiateRegistration(WebhookRegistration webhookRegistration) {
        validateCallbackUrl(webhookRegistration.callbackUrl());
        validateListenUris(webhookRegistration.listenUris());
    }

    private void validateListenUris(Set<String> listeningUris) {
        listeningUris.forEach(listeningUri -> {
            if (!uriCache.uriIsValid(listeningUri))
                throw new InvalidListeningUriException("Listening uri is not a valid endpoint: %s".formatted(listeningUri));
        });
    }

    private void validateCallbackUrl(String callbackUrl) {
        if (notValidUrl(callbackUrl))
            throw new InvalidUrlException("Callback url is not a valid url: %s".formatted(callbackUrl));

        if (!callbackUrl.toLowerCase().startsWith("https://"))
            throw new InvalidUrlException("Callback url does not start with https: %s".formatted(callbackUrl));
    }

    private boolean notValidUrl(String callbackUrl) {
        try {
            new URL(callbackUrl).toURI();
            return true;
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

}
