package no.fintlabs.provider.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRegistrationValidator webhookRegistrationValidator;
    private final Map<String, Set<String>> uriToCallBackUriMap = new ConcurrentHashMap<>();

    public void register(WebhookRegistration webhookRegistration) {
        webhookRegistrationValidator.valiateRegistration(webhookRegistration);
        webhookRegistration.listenUris().forEach(listenUri ->
                uriToCallBackUriMap
                        .computeIfAbsent(listenUri, k -> new HashSet<>())
                        .add(webhookRegistration.callbackUrl())
        );
    }

}
