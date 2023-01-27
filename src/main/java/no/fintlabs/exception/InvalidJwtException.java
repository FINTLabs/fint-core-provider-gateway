package no.fintlabs.exception;

public class InvalidJwtException extends Exception{
    public InvalidJwtException(String message) {
        super(message);
    }
}
