package no.fintlabs.provider

import com.fasterxml.jackson.databind.ObjectMapper
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.adapter.models.AdapterHeartbeat
import no.fintlabs.adapter.models.sync.DeleteSyncPage
import no.fintlabs.adapter.models.sync.DeltaSyncPage
import no.fintlabs.adapter.models.sync.FullSyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.novari.resource.server.authentication.CorePrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EmbeddedKafka(partitions = 1)
class ProviderControllerIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc
    private lateinit var mockPrincipal: CorePrincipal

    private val domainName = "utdanning"
    private val packageName = "elev"
    private val resourceName = "elev"
    private val orgId = "test.org.no"
    private val username = "test@adapter.$orgId"
    private val adapterId = "https://test.com/$orgId/$domainName/$packageName"

    @BeforeEach
    fun setup() {
        val jwt = Jwt.withTokenValue("mock-token-value")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("cn", username)
            .claim("fintAssetIDs", orgId)
            .claim("scope", listOf("fint-adapter"))
            .claim("Roles", listOf("FINT_Adapter_${domainName}_${packageName}"))
            .build()

        mockPrincipal = CorePrincipal(jwt, listOf(SimpleGrantedAuthority("ROLE_ADAPTER")))

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @Test
    fun `OpenAPI docs endpoint returns the spec without authentication`() {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.openapi").exists())
            .andExpect(jsonPath("$.paths").exists())
    }

    @Test
    fun `Actuator health endpoint reports UP without authentication`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    @Test
    fun `Swagger UI paths are not blocked by security`() {
        listOf("/swagger-ui", "/swagger-ui/index.html", "/swagger-ui/swagger-ui.css").forEach { path ->
            mockMvc.perform(get(path))
                .andExpect { result ->
                    val code = result.response.status
                    check(code != 401 && code != 403) { "expected $path to be open, got $code" }
                }
        }
    }

    @Test
    fun `Status endpoint should return 200 with CorePrincipal`() {
        mockMvc.perform(
            get("/status").with(authentication(mockPrincipal))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("Greetings form FINTLabs 👋"))
            .andExpect(jsonPath("$.corePrincipal.username").value(username))
    }

    @Test
    @Disabled
    // TODO: This is a bug, get the test to work
    fun `Should reject sync request if adapter is not registered`() {
        val syncPage = FullSyncPage().apply {
            this.metadata = SyncPageMetadata.builder()
                .adapterId(this@ProviderControllerIntegrationTest.adapterId)
                .orgId(this@ProviderControllerIntegrationTest.orgId)
                .corrId(UUID.randomUUID().toString())
                .totalSize(0)
                .page(0)
                .pageSize(0)
                .totalPages(1)
                .uriRef("/$domainName/$packageName/$resourceName")
                .time(System.currentTimeMillis())
                .build()
            this.resources = emptyList()
        }

        mockMvc.perform(
            post("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(authentication(mockPrincipal))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `Should successfully register adapter and perform fullSync`() {
        val capability = AdapterCapability().apply {
            this.domainName = this@ProviderControllerIntegrationTest.domainName
            this.packageName = this@ProviderControllerIntegrationTest.packageName
            this.resourceName = this@ProviderControllerIntegrationTest.resourceName
            this.fullSyncIntervalInDays = 1
            this.deltaSyncInterval = AdapterCapability.DeltaSyncInterval.IMMEDIATE
        }

        val adapterContract = AdapterContract().apply {
            this.adapterId = this@ProviderControllerIntegrationTest.adapterId
            this.orgId = this@ProviderControllerIntegrationTest.orgId
            this.username = this@ProviderControllerIntegrationTest.username
            this.heartbeatIntervalInMinutes = 5
            this.capabilities = setOf(capability)
        }

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(adapterContract))
                .with(authentication(mockPrincipal))
        ).andExpect(status().isOk)

        val syncPage = FullSyncPage().apply {
            this.metadata = SyncPageMetadata.builder()
                .adapterId(this@ProviderControllerIntegrationTest.adapterId)
                .orgId(this@ProviderControllerIntegrationTest.orgId)
                .corrId(UUID.randomUUID().toString())
                .totalSize(2)
                .page(0)
                .pageSize(2)
                .totalPages(1)
                .uriRef("/$domainName/$packageName/$resourceName")
                .time(System.currentTimeMillis())
                .build()
            this.resources = listOf(
                SyncPageEntry.of("${domainName}.${packageName}.${resourceName}/systemid/1", mapOf("name" to "Test1")),
                SyncPageEntry.of("${domainName}.${packageName}.${resourceName}/systemid/2", mapOf("name" to "Test2"))
            )
        }

        mockMvc.perform(
            post("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(authentication(mockPrincipal))
        ).andExpect(status().isCreated)
    }

    @Test
    fun `Should successfully perform deltaSync`() {
        registerAdapter()

        val syncPage = DeltaSyncPage().apply {
            this.metadata = SyncPageMetadata.builder()
                .adapterId(this@ProviderControllerIntegrationTest.adapterId)
                .orgId(this@ProviderControllerIntegrationTest.orgId)
                .corrId(UUID.randomUUID().toString())
                .totalSize(1)
                .page(0)
                .pageSize(1)
                .totalPages(1)
                .uriRef("/$domainName/$packageName/$resourceName")
                .time(System.currentTimeMillis())
                .build()
            this.resources = listOf(
                SyncPageEntry.of("$domainName.$packageName.$resourceName/systemid/1", mapOf("name" to "Updated"))
            )
        }

        mockMvc.perform(
            patch("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(authentication(mockPrincipal))
        ).andExpect(status().isCreated)
    }

    @Test
    fun `Should successfully perform deleteSync`() {
        registerAdapter()

        val syncPage = DeleteSyncPage().apply {
            this.metadata = SyncPageMetadata.builder()
                .adapterId(this@ProviderControllerIntegrationTest.adapterId)
                .orgId(this@ProviderControllerIntegrationTest.orgId)
                .corrId(UUID.randomUUID().toString())
                .totalSize(1)
                .page(0)
                .pageSize(1)
                .totalPages(1)
                .uriRef("/$domainName/$packageName/$resourceName")
                .time(System.currentTimeMillis())
                .build()
            this.resources = listOf(
                SyncPageEntry.of("$domainName.$packageName.$resourceName/systemid/1", mapOf("name" to "ToDelete"))
            )
        }

        mockMvc.perform(
            delete("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(authentication(mockPrincipal))
        ).andExpect(status().isOk)
    }

    @Test
    fun `Should successfully send heartbeat`() {
        val heartbeat = AdapterHeartbeat().apply {
            this.adapterId = this@ProviderControllerIntegrationTest.adapterId
            this.orgId = this@ProviderControllerIntegrationTest.orgId
            this.username = this@ProviderControllerIntegrationTest.username
        }

        mockMvc.perform(
            post("/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(heartbeat))
                .with(authentication(mockPrincipal))
        ).andExpect(status().isOk)
    }

    @Test
    fun `Should reject sync when orgId does not match JWT`() {
        registerAdapter()

        val syncPage = FullSyncPage().apply {
            this.metadata = SyncPageMetadata.builder()
                .adapterId(this@ProviderControllerIntegrationTest.adapterId)
                .orgId("wrong.org.no")
                .corrId(UUID.randomUUID().toString())
                .totalSize(0)
                .page(0)
                .pageSize(0)
                .totalPages(1)
                .time(System.currentTimeMillis())
                .build()
            this.resources = emptyList()
        }

        mockMvc.perform(
            post("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(authentication(mockPrincipal))
        ).andExpect(status().isForbidden)
    }

    private fun registerAdapter() {
        val capability = AdapterCapability().apply {
            this.domainName = this@ProviderControllerIntegrationTest.domainName
            this.packageName = this@ProviderControllerIntegrationTest.packageName
            this.resourceName = this@ProviderControllerIntegrationTest.resourceName
            this.fullSyncIntervalInDays = 1
            this.deltaSyncInterval = AdapterCapability.DeltaSyncInterval.IMMEDIATE
        }

        val adapterContract = AdapterContract().apply {
            this.adapterId = this@ProviderControllerIntegrationTest.adapterId
            this.orgId = this@ProviderControllerIntegrationTest.orgId
            this.username = this@ProviderControllerIntegrationTest.username
            this.heartbeatIntervalInMinutes = 5
            this.capabilities = setOf(capability)
        }

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(adapterContract))
                .with(authentication(mockPrincipal))
        ).andExpect(status().isOk)
    }

}
