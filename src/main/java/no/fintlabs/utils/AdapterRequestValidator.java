package no.fintlabs.utils;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.config.ProviderProperties;
import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class AdapterRequestValidator {

    private final ProviderProperties properties;

    public AdapterRequestValidator(ProviderProperties properties) {
        this.properties = properties;
    }

    public void validateRole(Jwt jwt, String domain, String packageName) {
        List<String> roles = jwt.getClaimAsStringList("Roles");
        if (resourcesDoesntMatchRoles(domain, packageName, roles)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resource does not match role");
        }
    }

    private static boolean resourcesDoesntMatchRoles(String domain, String packageName, List<String> roles) {
        return roles.stream().noneMatch(role -> role.contains("FINT_Adapter_%s_%s".formatted(domain, packageName)));
    }

    public void validateOrgId(Jwt jwt, String requestedOrgId) {
        if (properties.isResourceServerSecurityDisabled()) return;

        if (requestedOrgIdNotInFintAssetIDs(jwt, requestedOrgId)) {
            String message = String.format("OrgId %s is not a part of the authorized OrgIds for this adapter!", requestedOrgId);
            log.error(message);
            throw new InvalidOrgId(message);
        }
    }

    private static boolean requestedOrgIdNotInFintAssetIDs(Jwt jwt, String requestedOrgId) {
        return Stream.of(jwt.getClaims().get("fintAssetIDs").toString().split(","))
                .noneMatch(asset -> asset.equals(requestedOrgId));
    }

    public void validateUsername(Jwt jwt, String requestedUsername) {
        if (properties.isResourceServerSecurityDisabled()) return;

        if (!jwt.getClaims().get("cn").toString().equals(requestedUsername)) {
            String message = "Username in token is not the same as the username in the payload!";
            log.error(message);
            throw new InvalidUsername(message);
        }
    }
}
