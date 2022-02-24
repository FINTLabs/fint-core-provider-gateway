package no.fintlabs;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((authorize) -> authorize
                        .pathMatchers("/**").hasAnyAuthority("SCOPE_fint-client")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt(withDefaults())
                );
        return http.build();
    }

}