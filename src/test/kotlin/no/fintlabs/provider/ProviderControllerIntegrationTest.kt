package no.fintlabs.provider

import com.fasterxml.jackson.databind.ObjectMapper
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.adapter.models.AdapterHeartbeat
import no.fintlabs.adapter.models.sync.*
import no.fintlabs.provider.register.ContractJpaRepository
import no.fintlabs.provider.register.ContractService
import no.novari.resource.server.authentication.CorePrincipal
import org.apache.kafka.common.utils.Time
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EmbeddedKafka(partitions = 1)
@Import(TestcontainersConfiguration::class)
class ProviderControllerIntegrationTest @Autowired constructor(contractJpaRepository: ContractJpaRepository) {

    private val contractService: ContractService = ContractService(contractJpaRepository)

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
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    @Test
    fun `OpenAPI docs endpoint returns the spec without authentication`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.openapi").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.paths").exists())
    }

    @Test
    fun `Actuator health endpoint reports UP without authentication`() {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
    }

    @Test
    fun `Swagger UI paths are not blocked by security`() {
        listOf("/swagger-ui", "/swagger-ui/index.html", "/swagger-ui/swagger-ui.css").forEach { path ->
            mockMvc.perform(MockMvcRequestBuilders.get(path))
                .andExpect { result ->
                    val code = result.response.status
                    check(code != 401 && code != 403) { "expected $path to be open, got $code" }
                }
        }
    }

    @Test
    fun `Status endpoint should return 200 with CorePrincipal`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/status")
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("Greetings form FINTLabs 👋"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.corePrincipal.username").value(username))
    }

    @Test
    @Disabled
    // TODO: Enable in next iteration - where we enable contract validation
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
            MockMvcRequestBuilders.post("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
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
            MockMvcRequestBuilders.post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(adapterContract))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isOk)

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
            MockMvcRequestBuilders.post("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isCreated)
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
            MockMvcRequestBuilders.patch("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isCreated)
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
            MockMvcRequestBuilders.delete("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `Should successfully send heartbeat`() {
        val heartbeat = AdapterHeartbeat().apply {
            this.adapterId = this@ProviderControllerIntegrationTest.adapterId
            this.orgId = this@ProviderControllerIntegrationTest.orgId
            this.username = this@ProviderControllerIntegrationTest.username
        }

        mockMvc.perform(
            MockMvcRequestBuilders.post("/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(heartbeat))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isOk)
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
            MockMvcRequestBuilders.post("/$domainName/$packageName/$resourceName")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(syncPage))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `verify contracts get saved to database when registering adapter`() {
        registerAdapter()

        val adapterIds = contractService.getAdapterIds()

        assert(adapterIds.contains("https://test.com/test.org.no/utdanning/elev"))

    }

    @Test
    @Disabled("Will be enabled later")
    fun `should reject heartbeat when adapter is not registered`() {
        val heartbeat = AdapterHeartbeat().apply {
            this.adapterId = "random-adapter-id"
            this.username = "random-username"
            this.orgId = "whatEverOrgId"
            this.time = Time.SYSTEM.milliseconds()
        }

        mockMvc.perform(
            MockMvcRequestBuilders.post("/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(heartbeat))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `should accept heartbeat when adapter is registered`() {
        registerAdapter()
        val heartbeat = AdapterHeartbeat().apply {
            this.adapterId = this@ProviderControllerIntegrationTest.adapterId
            this.username = this@ProviderControllerIntegrationTest.username
            this.orgId = this@ProviderControllerIntegrationTest.orgId
            this.time = Time.SYSTEM.milliseconds()
        }

        mockMvc.perform(
            MockMvcRequestBuilders.post("/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(heartbeat))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `should accept registration when contract is missing capabilities`() {
        assertDoesNotThrow { registerAdapterWhitMissingCapabilities() }
    }

    @Test
    fun `should reject registration when fullSyncIntervalInDays is more than 7`() {
        registerAdapter(9).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `should reject registration when fullSyncIntervalInDays is less than 1`() {
        registerAdapter(0).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `should accept registration when fullSyncIntervalInDays is between 1 and 7`() {
        registerAdapter(4).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `should reject registration when component-resource is non-existing`() {
        registerAdapter(
            2,
            "utdanning",
            "elev",
            "non-existing-resource"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `should reject registration when capability alredy exsists for the organisatrion`() {
        contractService.saveContract(
            AdapterContract(
                "https://test.com/test.org.no/utdanning/elev",
                "test.org.no",
                "testeteste@adapter.fintlabs.no",
                5,
                setOf(
                    AdapterCapability(
                        "utdanning",
                        "elev",
                        "elev",
                        1,
                        AdapterCapability.DeltaSyncInterval.IMMEDIATE
                    )
                ),
                0L
            )
        )
        registerAdapter(3, "utdanning", "elev", "elev").andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `should reject when contract username does not match JWT username`() {
        registerAdapter(
            3,
            "utdanning",
            "elev",
            "elev",
            "sam@adapter.fintlabs.no",
            "sam@adapter.fintlabs.no/utdanning/elev"
        ).andExpect { MockMvcResultMatchers.status().isForbidden }
    }


    private fun registerAdapter(
        fullSyncIntervalInDays: Int? = null,
        domainName: String? = null,
        packageName: String? = null,
        resourceName: String? = null,
        adapterId: String? = null,
        username: String? = null,
    ): ResultActions {
        val capability = AdapterCapability().apply {
            this.domainName = domainName ?: this@ProviderControllerIntegrationTest.domainName
            this.packageName = packageName ?: this@ProviderControllerIntegrationTest.packageName
            this.resourceName = resourceName ?: this@ProviderControllerIntegrationTest.resourceName
            this.fullSyncIntervalInDays = fullSyncIntervalInDays ?: 1
            this.deltaSyncInterval = AdapterCapability.DeltaSyncInterval.IMMEDIATE
        }

        val adapterContract = AdapterContract().apply {
            this.adapterId = adapterId ?: this@ProviderControllerIntegrationTest.adapterId
            this.orgId = this@ProviderControllerIntegrationTest.orgId
            this.username = username ?: this@ProviderControllerIntegrationTest.username
            this.heartbeatIntervalInMinutes = 5
            this.capabilities = setOf(capability)
        }

        return mockMvc.perform(
            MockMvcRequestBuilders.post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(adapterContract))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        )
    }

    private fun registerAdapterWhitMissingCapabilities() {
        val adapterContract = AdapterContract().apply {
            this.adapterId = this@ProviderControllerIntegrationTest.adapterId
            this.orgId = this@ProviderControllerIntegrationTest.orgId
            this.username = this@ProviderControllerIntegrationTest.username
            this.heartbeatIntervalInMinutes = 5
        }

        mockMvc.perform(
            MockMvcRequestBuilders.post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(adapterContract))
                .with(SecurityMockMvcRequestPostProcessors.authentication(mockPrincipal))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

}