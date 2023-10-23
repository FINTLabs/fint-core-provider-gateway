package no.fintlabs.utils;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.resource.server.security.CorePrincipal;
import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Component
public class AdapterRequestValidator {

    @Value("${fint.security.enabled:true}")
    private boolean isSecurityEnabled;

    public void validateRole(CorePrincipal corePrincipal, String domain, String packageName) {
        if (!isSecurityEnabled) return;

        if (!corePrincipal.hasRole(String.format("FINT_Adapter_%s_%s", domain, packageName))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Resource does not match role");
        }
    }

    public void validateOrgId(CorePrincipal corePrincipal, String requestedOrgId) {
        if (!isSecurityEnabled) return;

        if (corePrincipal.getOrgId().equals(requestedOrgId)) {
            String message = String.format("OrgId %s is not a part of the authorized OrgIds for this adapter!", requestedOrgId);
            log.error(message);
            throw new InvalidOrgId(message);
        }
    }

    public void validateUsername(CorePrincipal corePrincipal, String requestedUsername) {
        if (!isSecurityEnabled) return;

        if (!corePrincipal.getUsername().equals(requestedUsername)) {
            String message = "Username in token is not the same as the username in the payload!";
            log.error(message);
            throw new InvalidUsername(message);
        }
    }

}
