package no.fintlabs.utils;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.exception.InvalidOrgId;
import no.fintlabs.exception.InvalidUsername;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class AdapterRequestValidator {

    public void validateRole(CorePrincipal corePrincipal, String domain, String packageName) {
        String role = String.format("FINT_Adapter_%s_%s", domain, packageName);
        if (corePrincipal.doesNotHaveRole(role)) {
            String message = String.format("%s: Role mismatch, user-roles: %s, check-role: %s", corePrincipal.getUsername(), corePrincipal.getRoles(), role);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    public void validateOrgId(CorePrincipal corePrincipal, String requestedOrgId) {
        if (corePrincipal.doesNotHaveMatchingOrgId(requestedOrgId)) {
            String message = String.format("%s: OrgId: [%s] is not a part of the authorized OrgIds for this adapter: [%s]", corePrincipal.getUsername(), requestedOrgId, corePrincipal.getOrgId());
            log.error(message);
            throw new InvalidOrgId(message);
        }
    }

    public void validateUsername(CorePrincipal corePrincipal, String requestedUsername) {
        if (corePrincipal.doesNotHaveMatchingUsername(requestedUsername)) {
            String message = String.format("%s: does not match the same username as request: %s", corePrincipal.getUsername(), requestedUsername);
            log.error(message);
            throw new InvalidUsername(message);
        }
    }

}
