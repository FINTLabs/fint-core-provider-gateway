package no.fintlabs.config;

import lombok.extern.slf4j.Slf4j;
import no.vigoiks.resourceserver.security.FintJwtCoreConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Slf4j
@Configuration
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
        log.info("Security: Normal web-filter-chain used. Authentication required.");

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
        log.warn("Security: Authentication NOT required. All users permitted.");

        return http
                .csrf(csrf -> csrf.disable()
                        .authorizeExchange(exchange -> exchange
                                .anyExchange()
                                .permitAll()))
                .build();
    }

    private String getScopeAuthority() {
        return String.format("SCOPE_%s", properties.getScope());
    }

}