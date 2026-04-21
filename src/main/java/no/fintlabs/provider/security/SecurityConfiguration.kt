package no.fintlabs.provider.security

import no.novari.resource.server.authentication.CorePrincipal
import no.novari.resource.server.converter.CorePrincipalConverter
import no.novari.resource.server.enums.FintScope
import no.novari.resource.server.enums.FintType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authorization.AuthorizationContext
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange { exchange ->
                exchange
                    .pathMatchers(*OPEN_PATHS).permitAll()
                    .pathMatchers(HttpMethod.POST, SYNC_PATH).access(::requireAdapterWithComponent)
                    .pathMatchers(HttpMethod.PATCH, SYNC_PATH).access(::requireAdapterWithComponent)
                    .pathMatchers(HttpMethod.DELETE, SYNC_PATH).access(::requireAdapterWithComponent)
                    .anyExchange().access(::requireAdapter)
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(
                        ReactiveJwtAuthenticationConverterAdapter(CorePrincipalConverter())
                    )
                }
            }
            .build()

    private fun requireAdapter(
        authentication: Mono<Authentication>,
        context: AuthorizationContext,
    ): Mono<AuthorizationDecision> =
        authentication
            .map { AuthorizationDecision(it.isFintAdapter()) }
            .defaultIfEmpty(AuthorizationDecision(false))

    private fun requireAdapterWithComponent(
        authentication: Mono<Authentication>,
        context: AuthorizationContext,
    ): Mono<AuthorizationDecision> =
        authentication
            .map { auth ->
                val domainName = context.variables["domainName"] as? String
                val pkg = context.variables["packageName"] as? String
                AuthorizationDecision(
                    auth.isFintAdapter() &&
                            domainName != null && pkg != null &&
                            (auth as CorePrincipal).hasComponent(domainName, pkg)
                )
            }
            .defaultIfEmpty(AuthorizationDecision(false))

    private fun Authentication.isFintAdapter(): Boolean =
        isAuthenticated && this is CorePrincipal && type == FintType.ADAPTER && FintScope.FINT_ADAPTER in scopes

    companion object {
        private const val SYNC_PATH = "/{domainName}/{packageName}/{entity}"
        private val OPEN_PATHS = arrayOf(
            "/api-docs/**",
            "/swagger/**",
            "/actuator/health",
            "/ready",
            "/offset",
        )
    }
}
