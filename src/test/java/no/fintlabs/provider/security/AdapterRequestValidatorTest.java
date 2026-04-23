package no.fintlabs.provider.security;

import no.novari.resource.server.authentication.CorePrincipal;
import no.fintlabs.provider.exception.InvalidOrgId;
import no.fintlabs.provider.exception.InvalidUsername;
import no.fintlabs.provider.register.ContractJpaRepository;
import no.fintlabs.provider.register.ContractService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class AdapterRequestValidatorTest {

    private final AdapterRequestValidator validator =
            new AdapterRequestValidator(mock(ContractService.class), mock(ContractJpaRepository.class));

    private CorePrincipal principal(String username, String assetIds) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("cn", username)
                .claim("fintAssetIDs", assetIds)
                .claim("scope", List.of("fint-adapter"))
                .build();
        return new CorePrincipal(jwt, List.of());
    }

    @Test
    public void shouldNotThrowWhenOrgIdInAssets() {
        CorePrincipal p = principal("test@adapter.test.org.no", "test.org.no");
        assertDoesNotThrow(() -> validator.validateOrgId(p, "test-org-no"));
    }

    @Test
    public void shouldThrowWhenOrgIdNotInAssets() {
        CorePrincipal p = principal("test@adapter.test.org.no", "test.org.no");
        assertThrows(InvalidOrgId.class, () -> validator.validateOrgId(p, "other-org-no"));
    }

    @Test
    public void shouldThrowWhenUsernameMismatch() {
        CorePrincipal p = principal("test@adapter.test.org.no", "test.org.no");
        assertThrows(InvalidUsername.class, () -> validator.validateUsername(p, "someone_else"));
    }

    @Test
    public void shouldNotThrowWhenUsernameMatches() {
        CorePrincipal p = principal("test@adapter.test.org.no", "test.org.no");
        assertDoesNotThrow(() -> validator.validateUsername(p, "test@adapter.test.org.no"));
    }
}
