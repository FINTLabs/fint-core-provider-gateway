package no.fintlabs.exception;

public class InvalidEventNameException extends RuntimeException {
    public InvalidEventNameException(String message) {
        super(message);
    }
}
