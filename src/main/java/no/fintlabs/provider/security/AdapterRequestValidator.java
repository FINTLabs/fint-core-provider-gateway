package no.fintlabs.provider.security;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.provider.exception.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterRequestValidator {

    private final AdapterContractContext adapterContractContext;

    public void validateAdapterId(CorePrincipal corePrincipal, String adapterId) {
        if (!adapterContractContext.userCanAccessAdapter(corePrincipal.getUsername(), adapterId)) {
            throw new UnauthorizedAdapterAccessException("Adapter is not registered with any contract");
        }
    }

    public void validateAdapterCapabilityPermission(String adapterId, String domainName, String packageName, String entityName) {
        if (!adapterContractContext.adapterCanPerformCapability(adapterId, domainName, packageName, entityName)) {
            log.warn("Validation failed: Adapter '{}' lacks capability to perform action on '{}-{}-{}'.", adapterId, domainName, packageName, entityName);
            throw new CapabilityNotSupportedException("Adapter lacks the necessary capabilities to perform this action");
        }
    }

    public void validateOrgId(CorePrincipal corePrincipal, String requestedOrgId) {
        if (corePrincipal.doesNotContainAsset(requestedOrgId.replace("-", ".").replace("_", "."))) {
            log.warn("Validation failed: JWT for user '{}' does not have access to organization '{}'. Available assets: {}", corePrincipal.getUsername(), requestedOrgId, corePrincipal.getAssets());
            throw new InvalidOrgId("Adapter assets does not contain the organization for the request");
        }
    }

    public void validateUsername(CorePrincipal corePrincipal, String contractUsername) {
        if (corePrincipal.doesNotHaveMatchingUsername(contractUsername)) {
            log.warn("Validation failed: Username mismatch. JWT's username '{}' does not match contract username '{}'.", corePrincipal.getUsername(), contractUsername);
            throw new InvalidUsername("Adapter username does not match contract username");
        }
    }

    public void validateRole(CorePrincipal corePrincipal, String domain, String packageName) {
        String role = String.format("FINT_Adapter_%s_%s", domain.toLowerCase(), packageName.toLowerCase());
        if (corePrincipal.doesNotHaveRole(role)) {
            log.warn("Validation failed: Principal '{}' is missing required role '{}'. Current roles: {}", corePrincipal.getName(), role, corePrincipal.getRoles());
            throw new MissingRoleException("Adapter does not have the correct role to perform this action");
        }
    }
}
