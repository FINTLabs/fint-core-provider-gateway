package no.fintlabs.provider.security

import jakarta.servlet.DispatcherType
import no.novari.resource.server.authentication.CorePrincipal
import no.novari.resource.server.converter.CorePrincipalConverter
import no.novari.resource.server.enums.FintScope
import no.novari.resource.server.enums.FintType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import java.util.function.Supplier

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { requests ->
                requests
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC, DispatcherType.FORWARD).permitAll()
                    .requestMatchers(*OPEN_PATHS.map { AntPathRequestMatcher.antMatcher(it) }.toTypedArray()).permitAll()
                    .requestMatchers(HttpMethod.POST, SYNC_PATH).access(requireAdapterWithComponent())
                    .requestMatchers(HttpMethod.PATCH, SYNC_PATH).access(requireAdapterWithComponent())
                    .requestMatchers(HttpMethod.DELETE, SYNC_PATH).access(requireAdapterWithComponent())
                    .anyRequest().access(requireAdapter())
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(CorePrincipalConverter())
                }
            }
            .build()

    private fun requireAdapter(): AuthorizationManager<RequestAuthorizationContext> =
        AuthorizationManager { authentication, _ ->
            AuthorizationDecision(authentication.resolve().isFintAdapter())
        }

    private fun requireAdapterWithComponent(): AuthorizationManager<RequestAuthorizationContext> =
        AuthorizationManager { authentication, context ->
            AuthorizationDecision(authentication.resolve().canAccessComponent(context))
        }

    private fun Supplier<Authentication?>.resolve(): Authentication? = runCatching { get() }.getOrNull()

    private fun Authentication?.canAccessComponent(context: RequestAuthorizationContext): Boolean {
        if (this !is CorePrincipal || !isFintAdapter()) return false
        val domainName = context.variables["domainName"] ?: return false
        val packageName = context.variables["packageName"] ?: return false
        return hasComponent(domainName, packageName)
    }

    private fun Authentication?.isFintAdapter(): Boolean =
        this != null && isAuthenticated && this is CorePrincipal && type == FintType.ADAPTER && FintScope.FINT_ADAPTER in scopes

    companion object {
        private const val SYNC_PATH = "/{domainName}/{packageName}/{entity}"
        private val OPEN_PATHS = arrayOf(
            "/api-docs",
            "/api-docs/**",
            "/swagger",
            "/swagger/**",
            "/swagger-ui",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**",
            "/actuator/health",
            "/ready",
            "/offset",
        )
    }
}
