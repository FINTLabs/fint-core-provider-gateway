package no.fintlabs.provider.register

import com.fasterxml.jackson.databind.ObjectMapper
import no.fintlabs.adapter.models.AdapterCapability
import no.fintlabs.adapter.models.AdapterContract
import no.fintlabs.provider.TestcontainersConfiguration
import no.novari.resource.server.authentication.CorePrincipal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EmbeddedKafka(partitions = 1)
@Import(TestcontainersConfiguration::class)
class ContractRegistrationIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var contractJpaRepository: ContractJpaRepository

    private lateinit var mockMvc: MockMvc
    private lateinit var principal: CorePrincipal

    private val orgId = "test.org.no"
    private val domainName = "utdanning"
    private val packageName = "elev"
    private val username = "test@adapter.$orgId"
    private val adapterId = "https://test.com/$orgId/$domainName/$packageName"

    @BeforeEach
    fun setup() {
        contractJpaRepository.deleteAll()

        val jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("cn", username)
            .claim("fintAssetIDs", orgId)
            .claim("scope", listOf("fint-adapter"))
            .claim("Roles", listOf("FINT_Adapter_${domainName}_${packageName}"))
            .build()
        principal = CorePrincipal(jwt, listOf(SimpleGrantedAuthority("ROLE_ADAPTER")))

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @Test
    fun `register persists contract and capabilities`() {
        postRegister(contract(heartbeat = 5, capabilities = setOf(capability(resource = "elev"))))

        val stored = contractJpaRepository.findByUserNameWithCapabilities(username).orElseThrow()

        assertThat(stored.userName).isEqualTo(username)
        assertThat(stored.adapterId).isEqualTo(adapterId)
        assertThat(stored.orgId).isEqualTo(orgId)
        assertThat(stored.heartbeatIntervalInMinutes).isEqualTo(5)
        assertThat(stored.capabilityEntityset)
            .singleElement()
            .satisfies({
                assertThat(it.domainName).isEqualTo(domainName)
                assertThat(it.pkgName).isEqualTo(packageName)
                assertThat(it.resourceName).isEqualTo("elev")
                assertThat(it.fullSyncIntervalInDays).isEqualTo(1)
                assertThat(it.deltaSyncInterval).isEqualTo("IMMEDIATE")
            })
    }

    @Test
    fun `re-registering with a different capability set replaces the old capabilities`() {
        postRegister(contract(capabilities = setOf(capability(resource = "elev"))))
        postRegister(
            contract(
                capabilities = setOf(
                    capability(pkg = "vurdering", resource = "elevfravar"),
                )
            )
        )

        val stored = contractJpaRepository.findByUserNameWithCapabilities(username).orElseThrow()

        assertThat(stored.capabilityEntityset.map { it.resourceName })
            .containsExactly("elevfravar")
    }

    @Test
    fun `re-registering updates scalar contract fields`() {
        postRegister(contract(heartbeat = 5))
        postRegister(contract(heartbeat = 15))

        val stored = contractJpaRepository.findById(username).orElseThrow()

        assertThat(stored.heartbeatIntervalInMinutes).isEqualTo(15)
        assertThat(contractJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `re-registering with no capabilities clears the capability set`() {
        postRegister(contract(capabilities = setOf(capability(resource = "elev"))))
        postRegister(contract(capabilities = emptySet()))

        val stored = contractJpaRepository.findByUserNameWithCapabilities(username).orElseThrow()

        assertThat(stored.capabilityEntityset).isEmpty()
    }

    private fun postRegister(contract: AdapterContract) {
        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(contract))
                .with(authentication(principal))
        ).andExpect(status().isOk)
    }

    private fun contract(
        heartbeat: Int = 5,
        capabilities: Set<AdapterCapability> = setOf(capability(resource = "elev")),
    ): AdapterContract = AdapterContract().apply {
        this.adapterId = this@ContractRegistrationIntegrationTest.adapterId
        this.orgId = this@ContractRegistrationIntegrationTest.orgId
        this.username = this@ContractRegistrationIntegrationTest.username
        this.heartbeatIntervalInMinutes = heartbeat
        this.capabilities = capabilities
    }

    private fun capability(
        domain: String = domainName,
        pkg: String = packageName,
        resource: String,
    ): AdapterCapability = AdapterCapability().apply {
        this.domainName = domain
        this.packageName = pkg
        this.resourceName = resource
        this.fullSyncIntervalInDays = 1
        this.deltaSyncInterval = AdapterCapability.DeltaSyncInterval.IMMEDIATE
    }
}
