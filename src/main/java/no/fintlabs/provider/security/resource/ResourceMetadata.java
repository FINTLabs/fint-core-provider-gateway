package no.fintlabs.provider.security.resource;

public record ResourceMetadata(
        String domainName,
        String packageName,
        String resourceName,
        boolean writeable
) {
}
