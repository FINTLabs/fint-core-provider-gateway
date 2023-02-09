package no.fintlabs.config;

import no.vigoiks.resourceserver.security.FintJwtCoreConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfiguration {

    private final ProviderProperties properties;

    public SecurityConfiguration(ProviderProperties properties) {
        this.properties = properties;
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http
            /*@Value("${fint.security.resourceserver.disabled:false}") boolean disabled*/
    ) {
        return properties.isResourceServerSecurityDisabled()
                ? createPermitAllFilterChain(http)
                : createOauth2FilterChain(http);
    }

    //@Bean
    private SecurityWebFilterChain createOauth2FilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((authorize) -> authorize
                        .pathMatchers("/**").hasAnyAuthority(getScopeAuthority())
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt()
                        .jwtAuthenticationConverter(new FintJwtCoreConverter()));
        return http.build();
    }

    private SecurityWebFilterChain createPermitAllFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .authorizeExchange()
                .anyExchange()
                .permitAll()
                .and()
                .build();
    }

    private String getScopeAuthority() {
        return String.format("SCOPE_%s", properties.getScope());
    }

}