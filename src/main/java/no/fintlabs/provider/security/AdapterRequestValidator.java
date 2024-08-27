package no.fintlabs.provider.security;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.provider.exception.InvalidOrgId;
import no.fintlabs.provider.exception.InvalidUsername;
import no.fintlabs.provider.exception.UnauthorizedAdapterAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterRequestValidator {

    private final AdapterContractContext adapterContractContext;

    // Validate username og ValidateAdapterId er egentlig det samme, men metadata i Syncpage mangler username felt
    public void validateAdapterId(CorePrincipal corePrincipal, String adapterId) {
        if (!adapterContractContext.userCanAccessAdapter(corePrincipal.getUsername(), adapterId)) {
            String message = "Username: %s does not belong to the adapterId: %s".formatted(corePrincipal.getUsername(), adapterId);
            log.error(message);
            throw new UnauthorizedAdapterAccessException(message);
        }
    }

    public void validateOrgId(CorePrincipal corePrincipal, String requestedOrgId) {
        if (corePrincipal.doesNotContainAsset(requestedOrgId.replace("-", ".").replace("_", "."))) {
            String message = String.format("%s: OrgId: %s is not a part of the authorized Assets for this adapter: %s", corePrincipal.getUsername(), requestedOrgId, corePrincipal.getAssets());
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

    public void validateRole(CorePrincipal corePrincipal, String domain, String packageName) {
        String role = String.format("FINT_Adapter_%s_%s", domain.toLowerCase(), packageName.toLowerCase());
        if (corePrincipal.doesNotHaveRole(role)) {
            String message = String.format("%s: Role mismatch, user-roles: %s, check-role: %s", corePrincipal.getUsername(), corePrincipal.getRoles(), role);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }

    }

}
