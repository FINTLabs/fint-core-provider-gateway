package no.fintlabs.provider.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/register")
    public ResponseEntity<WebhookRegistration> register(@RequestBody WebhookRegistration webhookRegistration) {
        webhookService.register(webhookRegistration);
        return ResponseEntity.ok(webhookRegistration);
    }

}
