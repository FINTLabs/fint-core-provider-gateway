package no.fintlabs.utils;


import no.fintlabs.config.ProviderProperties;
import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class AdapterRequestValidator {

    private final ProviderProperties properties;

    public AdapterRequestValidator(ProviderProperties properties) {
        this.properties = properties;
    }

    public  void validateOrgId(Jwt jwt, String requestedOrgId) {
        if (properties.isResourceServerSecurityDisabled()) return;

        if (requestedOrgIdNotInFintAssetIDs(jwt, requestedOrgId)) {
            throw new InvalidOrgId(String.format("OrgId %s is not a part of the authorized OrgIds for this adapter!", requestedOrgId));
        }
    }

    private static boolean requestedOrgIdNotInFintAssetIDs(Jwt jwt, String requestedOrgId) {
        return Stream.of(jwt.getClaims().get("fintAssetIDs").toString().split(","))
                .noneMatch(asset -> asset.equals(requestedOrgId));
    }

    public  void validateUsername(Jwt jwt, String requestedUsername) {
        if (properties.isResourceServerSecurityDisabled()) return;

        if (!jwt.getClaims().get("cn").toString().equals(requestedUsername)) {
            throw new InvalidUsername("Username in token is not the same as the username in the payload!");
        }
    }
}
