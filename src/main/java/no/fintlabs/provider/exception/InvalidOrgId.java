package no.fintlabs.provider.exception;

import lombok.Getter;

@Getter
public class InvalidOrgId extends RuntimeException {
    private final String message;

    public InvalidOrgId(String message) {
        this.message = message;
    }
}
