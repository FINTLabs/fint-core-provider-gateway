package no.fintlabs;


import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.stream.Stream;

public class AdapterRequestValidator {

    public static void validateOrgId(Jwt jwt, String requestedOrgId) {
        if (Stream.of(jwt.getClaims().get("fintAssetIDs").toString().split(",")).noneMatch(asset -> asset.equals(requestedOrgId))) {
            throw new InvalidOrgId(
                    String.format(
                            "OrgId %s is not a part of the authorized OrgIds for this adapter!",
                            requestedOrgId
                    )
            );
        }
    }

    public static void validateUsername(Jwt jwt, String requestedUsername) {
        if (!jwt.getClaims().get("cn").toString().equals(requestedUsername)) {
            throw new InvalidUsername("Username in token is not the same as the username in the payload!");
        }
    }
}
