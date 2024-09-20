package no.fintlabs.provider.webhook.uri;

public record Metadata(
        String domainName,
        String packageName,
        String resourceName
) {
}
