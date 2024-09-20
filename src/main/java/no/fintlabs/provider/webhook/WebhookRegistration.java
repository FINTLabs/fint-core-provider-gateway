package no.fintlabs.provider.webhook;

import java.util.Set;

public record WebhookRegistration(
        String callbackUrl,
        Set<String> listenUris
) {
}
