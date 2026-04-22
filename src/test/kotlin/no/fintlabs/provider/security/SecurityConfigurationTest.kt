package no.fintlabs.provider.security

import no.novari.kafka.KafkaConfiguration
import no.novari.resource.server.authentication.CorePrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    classes = [SecurityConfigurationTest.TestApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
)
@ActiveProfiles(SecurityConfigurationTest.PROFILE)
class SecurityConfigurationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/swagger-ui",
            "/swagger-ui/index.html",
            "/swagger-ui/swagger-ui.css",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/swagger-config",
            "/actuator/health",
        ],
    )
    fun `open paths are reachable without authentication even when no handler exists`(path: String) {
        mockMvc.perform(get(path))
            .andExpect { result ->
                val code = result.response.status
                check(code != 401 && code != 403) { "expected $path to be open, got $code" }
            }
    }

    @Test
    fun `unauthenticated request to protected path returns 401`() {
        mockMvc.perform(get("/status"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `client principal is denied on protected path`() {
        mockMvc.perform(
            get("/status")
                .with(authentication(principal(cn = "client@client.fintlabs.no", scope = "fint-client")))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `adapter without fint-adapter scope is denied`() {
        mockMvc.perform(
            get("/status").with(authentication(adapter(scope = "fint-client")))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `adapter with fint-adapter scope passes baseline check`() {
        mockMvc.perform(
            get("/status").with(authentication(adapter()))
        ).andExpect(status().isOk)
    }

    @Test
    fun `sync endpoint denies adapter without matching component`() {
        mockMvc.perform(
            post("/utdanning/vurdering/elev")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(authentication(adapter(roles = listOf("FINT_Adapter_utdanning_elev"))))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `sync endpoint allows adapter with matching component`() {
        mockMvc.perform(
            post("/utdanning/elev/elev")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(authentication(adapter(roles = listOf("FINT_Adapter_utdanning_elev"))))
        ).andExpect(status().isOk)
    }

    @Test
    fun `event POST is unauthenticated rejected`() {
        mockMvc.perform(
            post("/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `event POST denies client scope`() {
        mockMvc.perform(
            post("/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(authentication(principal(cn = "client@client.fintlabs.no", scope = "fint-client")))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `event POST passes filter chain for any fint-adapter regardless of roles`() {
        mockMvc.perform(
            post("/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(authentication(adapter()))
        ).andExpect(status().isOk)
    }

    @Test
    fun `event GET domain passes filter chain for any fint-adapter regardless of roles`() {
        mockMvc.perform(
            get("/event/utdanning").with(authentication(adapter()))
        ).andExpect(status().isOk)
    }

    @Test
    fun `event GET domain-package passes filter chain even when component does not match roles`() {
        mockMvc.perform(
            get("/event/utdanning/vurdering")
                .with(authentication(adapter(roles = listOf("FINT_Adapter_utdanning_elev"))))
        ).andExpect(status().isOk)
    }

    @Test
    fun `event GET domain-package-resource passes filter chain for any fint-adapter`() {
        mockMvc.perform(
            get("/event/utdanning/elev/elev").with(authentication(adapter()))
        ).andExpect(status().isOk)
    }

    @Test
    fun `event GET denies client scope`() {
        mockMvc.perform(
            get("/event/utdanning")
                .with(authentication(principal(cn = "client@client.fintlabs.no", scope = "fint-client")))
        ).andExpect(status().isForbidden)
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
        @GetMapping("/status")
        fun status(): String = "ok"

        @PostMapping("/{domainName}/{packageName}/{entity}")
        fun sync(
            @PathVariable domainName: String,
            @PathVariable packageName: String,
            @PathVariable entity: String,
        ): String = "$domainName/$packageName/$entity"

        @PostMapping("/event")
        fun postEvent(): String = "ok"

        @GetMapping("/event/{domainName}")
        fun getEventsDomain(@PathVariable domainName: String): String = domainName

        @GetMapping("/event/{domainName}/{packageName}")
        fun getEventsPackage(
            @PathVariable domainName: String,
            @PathVariable packageName: String,
        ): String = "$domainName/$packageName"

        @GetMapping("/event/{domainName}/{packageName}/{resourceName}")
        fun getEventsResource(
            @PathVariable domainName: String,
            @PathVariable packageName: String,
            @PathVariable resourceName: String,
        ): String = "$domainName/$packageName/$resourceName"
    }

    companion object {
        const val PROFILE = "security-config-test"
    }
}
