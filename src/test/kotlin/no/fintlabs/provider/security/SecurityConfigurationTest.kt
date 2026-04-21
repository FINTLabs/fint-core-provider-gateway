package no.fintlabs.provider.security

import no.novari.kafka.KafkaConfiguration
import no.novari.resource.server.authentication.CorePrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootTest(
    classes = [SecurityConfigurationTest.TestApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
)
@ActiveProfiles(SecurityConfigurationTest.PROFILE)
class SecurityConfigurationTest {

    @Autowired
    private lateinit var context: ApplicationContext

    @MockitoBean
    private lateinit var jwtDecoder: ReactiveJwtDecoder

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setup() {
        client = WebTestClient
            .bindToApplicationContext(context)
            .apply(springSecurity())
            .configureClient()
            .build()
    }

    @Test
    fun `open path is reachable without authentication`() {
        client.get().uri("/ready").exchange()
            .expectStatus().isOk
    }

    @Test
    fun `unauthenticated request to protected path returns 401`() {
        client.get().uri("/status").exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `client principal is denied on protected path`() {
        client.mutateWith(mockAuthentication(principal(cn = "client@client.fintlabs.no", scope = "fint-client")))
            .get().uri("/status").exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `adapter without fint-adapter scope is denied`() {
        client.mutateWith(mockAuthentication(adapter(scope = "fint-client")))
            .get().uri("/status").exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `adapter with fint-adapter scope passes baseline check`() {
        client.mutateWith(mockAuthentication(adapter()))
            .get().uri("/status").exchange()
            .expectStatus().isOk
    }

    @Test
    fun `sync endpoint denies adapter without matching component`() {
        client.mutateWith(mockAuthentication(adapter(roles = listOf("FINT_Adapter_utdanning_elev"))))
            .post().uri("/utdanning/vurdering/elev")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `sync endpoint allows adapter with matching component`() {
        client.mutateWith(mockAuthentication(adapter(roles = listOf("FINT_Adapter_utdanning_elev"))))
            .post().uri("/utdanning/elev/elev")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isOk
    }

    private fun adapter(
        scope: String = "fint-adapter",
        roles: List<String> = emptyList(),
    ): CorePrincipal = principal(
        cn = "test@adapter.fintlabs.no",
        scope = scope,
        roles = roles,
    )

    private fun principal(
        cn: String,
        scope: String,
        roles: List<String> = emptyList(),
    ): CorePrincipal {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("cn", cn)
            .claim("fintAssetIDs", "fintlabs.no")
            .claim("scope", listOf(scope))
            .claim("Roles", roles)
            .build()
        return CorePrincipal(jwt, emptyList())
    }

    @Configuration
    @Profile(PROFILE)
    @EnableAutoConfiguration(exclude = [KafkaAutoConfiguration::class, KafkaConfiguration::class])
    @Import(SecurityConfiguration::class, Endpoints::class)
    class TestApp

    @RestController
    @Profile(PROFILE)
    class Endpoints {
        @GetMapping("/ready")
        fun ready(): String = "ok"

        @GetMapping("/status")
        fun status(): String = "ok"

        @PostMapping("/{domain}/{packageName}/{entity}")
        fun sync(
            @PathVariable domain: String,
            @PathVariable packageName: String,
            @PathVariable entity: String,
        ): String = "$domain/$packageName/$entity"
    }

    companion object {
        const val PROFILE = "security-config-test"
    }
}
