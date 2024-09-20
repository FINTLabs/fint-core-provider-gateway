package no.fintlabs.provider.webhook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidListeningUriException extends RuntimeException {
    public InvalidListeningUriException(String message) {
        super(message);
    }
}
