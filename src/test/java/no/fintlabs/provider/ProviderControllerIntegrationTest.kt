package no.fintlabs.provider

import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.adapter.models.AdapterHeartbeat
import no.fintlabs.adapter.models.sync.DeleteSyncPage
import no.fintlabs.adapter.models.sync.DeltaSyncPage
import no.fintlabs.adapter.models.sync.FullSyncPage
import no.fintlabs.adapter.models.sync.SyncPageEntry
import no.fintlabs.adapter.models.sync.SyncPageMetadata
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal
import no.fintlabs.provider.register.ContractJpaRepository
import no.fintlabs.provider.register.ContractService
import org.apache.kafka.common.utils.Time
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1)
class ProviderControllerIntegrationTest @Autowired constructor(contractJpaRepository: ContractJpaRepository) {

    private val contractService: ContractService = ContractService(contractJpaRepository)

    @Autowired
    private lateinit var context: ApplicationContext

    private lateinit var client: WebTestClient
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

        client = WebTestClient
            .bindToApplicationContext(context)
            .apply(springSecurity())
            .configureClient()
            .build()
    }

    @Test
    fun `Status endpoint should return 200 with CorePrincipal`() {
        client.mutateWith(mockAuthentication(mockPrincipal))
            .get()
            .uri("/status")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("Greetings form FINTLabs 👋")
            .jsonPath("$.corePrincipal.username").isEqualTo(username)
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

        client.mutateWith(mockAuthentication(mockPrincipal))
            .post()
            .uri("/$domainName/$packageName/$resourceName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(syncPage)
            .exchange()
            .expectStatus().isForbidden
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

        client.mutateWith(mockAuthentication(mockPrincipal))
            .post()
            .uri("/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(adapterContract)
            .exchange()
            .expectStatus().isOk

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

        client.mutateWith(mockAuthentication(mockPrincipal))
            .post()
            .uri("/$domainName/$packageName/$resourceName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(syncPage)
            .exchange()
            .expectStatus().isCreated
    }

    @Test
    fun `Should reject sync request if JWT lacks the required role`() {
        val invalidJwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .claim("cn", username)
            .claim("fintAssetIDs", orgId)
            .claim("scope", listOf("fint-adapter"))
            .claim("Roles", listOf("FINT_Adapter_wrong_role"))
            .build()

        val invalidPrincipal = CorePrincipal(invalidJwt, listOf(SimpleGrantedAuthority("ROLE_ADAPTER")))

        val syncPage = FullSyncPage().apply {
            this.metadata = SyncPageMetadata().apply {
                this.orgId = this@ProviderControllerIntegrationTest.orgId
                this.adapterId = this@ProviderControllerIntegrationTest.adapterId
            }
        }

        client.mutateWith(mockAuthentication(invalidPrincipal))
            .post()
            .uri("/$domainName/$packageName/$resourceName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(syncPage)
            .exchange()
            .expectStatus().isForbidden
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

        client.mutateWith(mockAuthentication(mockPrincipal))
            .patch()
            .uri("/$domainName/$packageName/$resourceName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(syncPage)
            .exchange()
            .expectStatus().isCreated
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

        client.mutateWith(mockAuthentication(mockPrincipal))
            .method(HttpMethod.DELETE)
            .uri("/$domainName/$packageName/$resourceName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(syncPage)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `Should successfully send heartbeat`() {
        val heartbeat = AdapterHeartbeat().apply {
            this.adapterId = this@ProviderControllerIntegrationTest.adapterId
            this.orgId = this@ProviderControllerIntegrationTest.orgId
            this.username = this@ProviderControllerIntegrationTest.username
        }

        client.mutateWith(mockAuthentication(mockPrincipal))
            .post()
            .uri("/heartbeat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(heartbeat)
            .exchange()
            .expectStatus().isOk
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

        client.mutateWith(mockAuthentication(mockPrincipal))
            .post()
            .uri("/$domainName/$packageName/$resourceName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(syncPage)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `verify contracts get saved to database when registering adapter`() {
        registerAdapter()

        val adapterIds = contractService.getAdapterIds()

        assert(adapterIds.contains("https://test.com/test.org.no/utdanning/elev"))

    }

    @Test
    fun `should reject heartbeat when adapter is not registered`() {
        val heartbeat = AdapterHeartbeat().apply {
            this.adapterId = this@ProviderControllerIntegrationTest.adapterId
            this.username = this@ProviderControllerIntegrationTest.username
            this.orgId = this@ProviderControllerIntegrationTest.orgId
            this.time = Time.SYSTEM.milliseconds()
        }
        client.mutateWith(mockAuthentication(mockPrincipal))
            .post()
            .uri("/heartbeat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(heartbeat)
            .exchange()
            .expectStatus().isForbidden
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
        client.mutateWith(mockAuthentication(mockPrincipal))
            .post()
            .uri("/heartbeat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(heartbeat)
            .exchange()
            .expectStatus().isOk
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

        client.mutateWith(mockAuthentication(mockPrincipal))
            .post()
            .uri("/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(adapterContract)
            .exchange()
            .expectStatus().isOk
    }

}