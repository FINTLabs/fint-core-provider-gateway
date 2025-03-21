package no.fintlabs.provider.kafka;

public record ProviderError(
        String name,
        String error,
        StackTraceElement[] stacktrace,
        Long time
) {
    public static ProviderError from(Throwable ex) {
        return new ProviderError(
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex.getStackTrace(),
                System.currentTimeMillis()
        );
    }
}

