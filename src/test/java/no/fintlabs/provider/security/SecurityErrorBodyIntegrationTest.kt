package no.fintlabs.provider.security

import com.fasterxml.jackson.databind.ObjectMapper
import no.fintlabs.adapter.models.AdapterContract
import no.novari.resource.server.authentication.CorePrincipal
import no.fintlabs.provider.TestcontainersConfiguration
import no.fintlabs.provider.kafka.ProviderError
import no.fintlabs.provider.kafka.ProviderErrorPublisher
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EmbeddedKafka(partitions = 1)
@Import(TestcontainersConfiguration::class)
class SecurityErrorBodyIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var providerErrorPublisher: ProviderErrorPublisher

    private lateinit var mockMvc: MockMvc

    private val orgId = "test.org.no"
    private val username = "test@adapter.$orgId"

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @Test
    fun `register without authentication returns 401 with ProblemDetail body`() {
        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(AdapterContract()))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").exists())
            .andExpect(jsonPath("$.instance").value("/register"))

        verify(providerErrorPublisher, times(1)).publish(any(ProviderError::class.java))
    }

    @Test
    fun `register with JWT missing fint-adapter scope returns 403 with ProblemDetail body`() {
        val jwt = Jwt.withTokenValue("mock-token-value")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("cn", username)
            .claim("fintAssetIDs", orgId)
            .claim("scope", listOf("some-other-scope"))
            .claim("Roles", listOf("FINT_Adapter_utdanning_elev"))
            .build()
        val principalWithoutAdapterScope = CorePrincipal(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")))

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(AdapterContract()))
                .with(authentication(principalWithoutAdapterScope))
        )
            .andExpect(status().isForbidden)
            .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.detail").value(containsString("fint-adapter")))
            .andExpect(jsonPath("$.instance").value("/register"))

        verify(providerErrorPublisher, times(1)).publish(any(ProviderError::class.java))
    }

    @Test
    fun `register with missing request body returns 400 with ProblemDetail explaining the issue`() {
        val jwt = Jwt.withTokenValue("mock-token-value")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("cn", username)
            .claim("fintAssetIDs", orgId)
            .claim("scope", listOf("fint-adapter"))
            .claim("Roles", listOf("FINT_Adapter_utdanning_elev"))
            .build()
        val adapterPrincipal = CorePrincipal(jwt, listOf(SimpleGrantedAuthority("ROLE_ADAPTER")))

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .with(authentication(adapterPrincipal))
        )
            .andExpect(status().isBadRequest)
            .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail").value("Required request body is missing"))
            .andExpect(jsonPath("$.instance").value("/register"))

        verify(providerErrorPublisher, times(1)).publish(any(ProviderError::class.java))
    }
}
