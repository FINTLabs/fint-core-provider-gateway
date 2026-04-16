package no.fintlabs.provider.security;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.provider.exception.*;
import no.fintlabs.provider.register.ContractJpaRepository;
import no.fintlabs.provider.register.ContractService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterRequestValidator {

    // TODO: Convert to Kotlin

    private final ContractService contractService;
    private final ContractJpaRepository contractJpaRepository;

    public void validateAdapterId(CorePrincipal corePrincipal, String adapterId) {
        if (!Boolean.TRUE.equals(contractService.userCanAccessAdapter(corePrincipal.getUsername()))) {
            throw new UnauthorizedAdapterAccessException("Adapter is not registered with any contract");
        }
    }

    public void validateAdapterCapabilityPermission(String username, String domainName, String packageName, String entityName) {
        if (!Boolean.TRUE.equals(contractService.adapterCanPerformCapability(username, domainName, packageName, entityName))) {
            log.warn("Validation failed: Adapter '{}' lacks capability to perform action on '{}-{}-{}'.", username, domainName, packageName, entityName);
            throw new CapabilityNotSupportedException("Adapter lacks the necessary capabilities to perform this action");
        }
    }

    public void validateOrgId(CorePrincipal corePrincipal, String requestedOrgId) {
        if (corePrincipal.doesNotContainAsset(requestedOrgId.replace("-", ".").replace("_", "."))) {
            log.warn("Validation failed: JWT for user '{}' does not have access to organization '{}'. Available assets: {}", corePrincipal.getUsername(), requestedOrgId, corePrincipal.getAssets());
            throw new InvalidOrgId("Adapter assets does not contain the organization for the request");
        }
    }

    public void isRegistered(CorePrincipal corePrincipal, String usernName) {
        if (corePrincipal.getUsername().equals(usernName)) {
            if (!contractJpaRepository.existsById(usernName)) {
                log.warn("Validation failed: Adapter with id '{}' is not registered", usernName);
                throw new InvalidOrgId("Adapter is not registered with any contract");
            }
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
            log.warn("Validation failed: Principal '{}' is missing required role '{}'. Current roles: {}", corePrincipal.getUsername(), role, corePrincipal.getRoles());
            throw new MissingRoleException("Adapter does not have the correct role to perform this action");
        }
    }
}
