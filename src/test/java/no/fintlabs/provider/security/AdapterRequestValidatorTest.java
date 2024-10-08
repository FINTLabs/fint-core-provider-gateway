package no.fintlabs.provider.security;

import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.provider.exception.MissingRoleException;
import no.fintlabs.provider.security.AdapterRequestValidator;
import no.fintlabs.provider.exception.InvalidUsername;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

public class AdapterRequestValidatorTest {

    @Mock
    private CorePrincipal corePrincipal;

    @InjectMocks
    private AdapterRequestValidator adapterRequestValidator;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void shouldThrowExceptionWhenRoleMismatch() {
        when(corePrincipal.doesNotHaveRole(anyString())).thenReturn(true);
        assertThrows(MissingRoleException.class, () -> adapterRequestValidator.validateRole(corePrincipal, "domain", "package"));
    }

    @Test
    public void shouldNotThrowExceptionWhenRoleMatch() {
        when(corePrincipal.doesNotHaveRole(anyString())).thenReturn(false);
        assertDoesNotThrow(() -> adapterRequestValidator.validateRole(corePrincipal, "domain", "package"));
    }

    @Test
    public void shouldNotThrowExceptionWhenOrgIdMatch() {
        when(corePrincipal.doesNotHaveMatchingOrgId(anyString())).thenReturn(false);
        assertDoesNotThrow(() -> adapterRequestValidator.validateOrgId(corePrincipal, "orgId"));
    }

    @Test
    public void shouldThrowExceptionWhenUsernameMismatch() {
        when(corePrincipal.doesNotHaveMatchingUsername(anyString())).thenReturn(true);
        assertThrows(InvalidUsername.class, () -> adapterRequestValidator.validateUsername(corePrincipal, "username"));
    }

    @Test
    public void shouldNotThrowExceptionWhenUsernameMatch() {
        when(corePrincipal.doesNotHaveMatchingUsername(anyString())).thenReturn(false);
        assertDoesNotThrow(() -> adapterRequestValidator.validateUsername(corePrincipal, "username"));
    }
}