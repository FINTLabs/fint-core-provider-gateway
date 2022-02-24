package no.fintlabs.exception;

import lombok.Getter;

@Getter
public class InvalidUsername extends RuntimeException {
    private final String message;

    public InvalidUsername(String message) {
        this.message = message;
    }
}
