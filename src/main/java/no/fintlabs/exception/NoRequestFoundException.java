package no.fintlabs.exception;

public class NoRequestFoundException extends Exception{

    public NoRequestFoundException(String corrId) {
        super("Could not found request with corr-id: " + corrId);
    }
}
