package no.fintlabs.provider.topic

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.provider.config.CleanupTopicsProperties
import no.fintlabs.provider.config.ComponentConfig
import no.fintlabs.provider.config.ProviderProperties
import no.fintlabs.provider.kafka.topic.TopicCleanupService
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNameParameters
import no.novari.kafka.topic.name.TopicNameService
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.DeleteTopicsResult
import org.apache.kafka.clients.admin.ListTopicsResult
import org.apache.kafka.common.KafkaFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaAdmin
import java.time.Duration

class TopicCleanupServiceTest {

    private lateinit var topicNameService: TopicNameService
    private lateinit var kafkaAdmin: KafkaAdmin
    private lateinit var adminClient: AdminClient

    private val defaultOrgId = "fintlabs-no"
    private val defaultDomainContext = "fint-core"

    @BeforeEach
    fun setup() {
        topicNameService = mockk()
        kafkaAdmin = mockk(relaxed = true)
        adminClient = mockk(relaxed = true)

        every { topicNameService.validateAndMapToTopicName(any()) } answers {
            val params = firstArg<TopicNameParameters>()
            val orgId = params.topicNamePrefixParameters.orgId ?: defaultOrgId
            val domainContext = params.topicNamePrefixParameters.domainContext ?: defaultDomainContext
            val suffix = params.topicNameSuffixParameters
                .mapNotNull { it.value }
                .joinToString(".")
            "$orgId.$domainContext.${params.messageType.topicNameParameter}.$suffix"
        }
    }

    private fun topicCleanupService(
        components: List<ComponentConfig> = emptyList(),
        batchSize: Int = 10,
        batchDelay: Duration = Duration.ZERO,
    ) = TopicCleanupService(
        ProviderProperties(components = components),
        CleanupTopicsProperties(
            enabled = true,
            batchSize = batchSize,
            batchDelay = batchDelay,
        ),
        topicNameService,
        kafkaAdmin,
    )

    private fun stubListTopics(topics: Set<String>) {
        val listResult = mockk<ListTopicsResult>()
        every { adminClient.listTopics() } returns listResult
        every { listResult.names() } returns KafkaFuture.completedFuture(topics)
    }

    private fun stubDeleteTopics(): DeleteTopicsResult {
        val deleteResult = mockk<DeleteTopicsResult>()
        every { adminClient.deleteTopics(any<Collection<String>>()) } returns deleteResult
        every { deleteResult.all() } returns KafkaFuture.completedFuture(null)
        return deleteResult
    }

    @Test
    fun `computeExpectedTopicNames produces entity, request and response topics per component-org pair`() {
        val components = listOf(
            ComponentConfig("utdanning", "elev", listOf("afk-no", "bfk-no"))
        )

        val expected = topicCleanupService(components).computeExpectedTopicNames()

        assertThat(expected).contains(
            "afk-no.fint-core.entity.utdanning-elev",
            "afk-no.fint-core.event.utdanning-elev-request",
            "afk-no.fint-core.event.utdanning-elev-response",
            "bfk-no.fint-core.entity.utdanning-elev",
            "bfk-no.fint-core.event.utdanning-elev-request",
            "bfk-no.fint-core.event.utdanning-elev-response",
        )
    }

    @Test
    fun `computeExpectedTopicNames includes relation-update topic only when component has the flag set`() {
        val components = listOf(
            ComponentConfig("utdanning", "elev", listOf("afk-no"), relationUpdate = true),
            ComponentConfig("utdanning", "ot", listOf("afk-no"), relationUpdate = false),
        )

        val expected = topicCleanupService(components).computeExpectedTopicNames()

        assertThat(expected).contains("afk-no.fint-core.entity.utdanning-elev-relation-update")
        assertThat(expected).doesNotContain("afk-no.fint-core.entity.utdanning-ot-relation-update")
    }

    @Test
    fun `computeExpectedTopicNames always includes the global event topics`() {
        val expected = topicCleanupService().computeExpectedTopicNames()

        assertThat(expected).contains(
            "$defaultOrgId.$defaultDomainContext.event.adapter-health",
            "$defaultOrgId.$defaultDomainContext.event.adapter-register",
            "$defaultOrgId.$defaultDomainContext.event.adapter-full-sync",
            "$defaultOrgId.$defaultDomainContext.event.adapter-delta-sync",
            "$defaultOrgId.$defaultDomainContext.event.adapter-delete-sync",
            "$defaultOrgId.$defaultDomainContext.event.consumer-error",
            "$defaultOrgId.$defaultDomainContext.event.sync-status",
        )
    }

    @Test
    fun `computeExpectedTopicNames uses correct parameters for entity and event builders`() {
        val components = listOf(
            ComponentConfig("utdanning", "elev", listOf("afk-no"), relationUpdate = true)
        )

        topicCleanupService(components).computeExpectedTopicNames()

        verify {
            topicNameService.validateAndMapToTopicName(match<EntityTopicNameParameters> {
                it.resourceName == "utdanning-elev" &&
                        it.topicNamePrefixParameters.orgId == "afk-no"
            })
            topicNameService.validateAndMapToTopicName(match<EntityTopicNameParameters> {
                it.resourceName == "utdanning-elev-relation-update" &&
                        it.topicNamePrefixParameters.orgId == "afk-no"
            })
            topicNameService.validateAndMapToTopicName(match<EventTopicNameParameters> {
                it.eventName == "utdanning-elev-request" &&
                        it.topicNamePrefixParameters.orgId == "afk-no"
            })
            topicNameService.validateAndMapToTopicName(match<EventTopicNameParameters> {
                it.eventName == "utdanning-elev-response" &&
                        it.topicNamePrefixParameters.orgId == "afk-no"
            })
        }
    }

    @Test
    fun `cleanup does nothing and skips listing when components are empty`() {
        val deleted = topicCleanupService().cleanup(adminClient)

        assertThat(deleted).isEmpty()
        verify(exactly = 0) { adminClient.listTopics() }
        verify(exactly = 0) { adminClient.deleteTopics(any<Collection<String>>()) }
    }

    @Test
    fun `cleanup only deletes topics belonging to orgs derived from components`() {
        val components = listOf(ComponentConfig("utdanning", "elev", listOf("afk-no")))
        val afkOrphan = "afk-no.fint-core.entity.utdanning-obsolete"
        val bfkOrphan = "bfk-no.fint-core.entity.utdanning-obsolete"

        stubListTopics(
            setOf(
                afkOrphan,
                bfkOrphan,
                "afk-no.fint-core.entity.utdanning-elev",
            )
        )
        stubDeleteTopics()

        val deleted = topicCleanupService(components).cleanup(adminClient)

        assertThat(deleted).containsExactly(afkOrphan)
        verify { adminClient.deleteTopics(match<Collection<String>> { it.toSet() == setOf(afkOrphan) }) }
    }

    @Test
    fun `cleanup requires the orgId prefix to match exactly so similar names are not swept up`() {
        val components = listOf(ComponentConfig("utdanning", "elev", listOf("afk-no")))
        val orphan = "afk-no.fint-core.entity.something-old"
        val neighbor = "afk-nord-no.fint-core.entity.something-old"

        stubListTopics(setOf(orphan, neighbor, "afk-no.fint-core.entity.utdanning-elev"))
        stubDeleteTopics()

        val deleted = topicCleanupService(components).cleanup(adminClient)

        assertThat(deleted).containsExactly(orphan)
        assertThat(deleted).doesNotContain(neighbor)
    }

    @Test
    fun `cleanup ignores topics that do not contain fint-core even within the org scope`() {
        val components = listOf(ComponentConfig("utdanning", "elev", listOf("afk-no")))
        val foreign = "afk-no.fint-example.entity.some-topic"
        val system = "__consumer_offsets"

        stubListTopics(setOf(foreign, system, "afk-no.fint-core.entity.utdanning-elev"))

        val deleted = topicCleanupService(components).cleanup(adminClient)

        assertThat(deleted).isEmpty()
        verify(exactly = 0) { adminClient.deleteTopics(any<Collection<String>>()) }
    }

    @Test
    fun `cleanup does not call deleteTopics when every fint-core topic in scope is expected`() {
        val components = listOf(ComponentConfig("utdanning", "elev", listOf("afk-no")))

        stubListTopics(
            setOf(
                "afk-no.fint-core.entity.utdanning-elev",
                "afk-no.fint-core.event.utdanning-elev-request",
                "afk-no.fint-core.event.utdanning-elev-response",
                "__consumer_offsets",
            )
        )

        val deleted = topicCleanupService(components).cleanup(adminClient)

        assertThat(deleted).isEmpty()
        verify(exactly = 0) { adminClient.deleteTopics(any<Collection<String>>()) }
    }

    @Test
    fun `cleanup aggregates orphans across multiple orgs into a single delete call`() {
        val components = listOf(
            ComponentConfig("utdanning", "elev", listOf("afk-no", "bfk-no"), relationUpdate = true)
        )
        val afkOrphan = "afk-no.fint-core.entity.utdanning-removed"
        val bfkOrphan = "bfk-no.fint-core.event.utdanning-elev-legacy-response"
        val nonFintCore = "afk-no.something-else.entity.stuff"
        val keptAfk = "afk-no.fint-core.entity.utdanning-elev"
        val keptBfk = "bfk-no.fint-core.entity.utdanning-elev-relation-update"

        stubListTopics(setOf(afkOrphan, bfkOrphan, nonFintCore, keptAfk, keptBfk))
        stubDeleteTopics()

        val deleted = topicCleanupService(components).cleanup(adminClient)

        assertThat(deleted).containsExactlyInAnyOrder(afkOrphan, bfkOrphan)
        verify {
            adminClient.deleteTopics(match<Collection<String>> {
                it.toSet() == setOf(afkOrphan, bfkOrphan)
            })
        }
    }

    @Test
    fun `cleanup returns empty when Kafka reports no topics for derived orgs`() {
        val components = listOf(ComponentConfig("utdanning", "elev", listOf("afk-no")))
        stubListTopics(emptySet())

        val deleted = topicCleanupService(components).cleanup(adminClient)

        assertThat(deleted).isEmpty()
        verify(exactly = 0) { adminClient.deleteTopics(any<Collection<String>>()) }
    }

    @Test
    fun `cleanup deletes an existing relation-update topic when the component flag is false`() {
        val components = listOf(
            ComponentConfig("utdanning", "elev", listOf("afk-no"), relationUpdate = false)
        )
        val staleRelationUpdate = "afk-no.fint-core.entity.utdanning-elev-relation-update"
        val keptEntity = "afk-no.fint-core.entity.utdanning-elev"

        stubListTopics(setOf(keptEntity, staleRelationUpdate))
        stubDeleteTopics()

        val deleted = topicCleanupService(components).cleanup(adminClient)

        assertThat(deleted).containsExactly(staleRelationUpdate)
    }

    @Test
    fun `cleanup splits deletes into batches when orphans exceed batch size`() {
        val components = listOf(ComponentConfig("utdanning", "elev", listOf("afk-no")))
        val orphans = (1..7).map { "afk-no.fint-core.entity.utdanning-old-$it" }.toSet()
        stubListTopics(orphans)
        stubDeleteTopics()

        val deleted = topicCleanupService(components, batchSize = 3).cleanup(adminClient)

        assertThat(deleted).containsExactlyInAnyOrderElementsOf(orphans)
        verify(exactly = 3) { adminClient.deleteTopics(any<Collection<String>>()) }
    }

    @Test
    fun `cleanup partitions orphans across batches so every orphan is deleted exactly once`() {
        val components = listOf(ComponentConfig("utdanning", "elev", listOf("afk-no")))
        val orphans = (1..5).map { "afk-no.fint-core.entity.utdanning-old-$it" }.toSet()
        stubListTopics(orphans)
        val seenBatches = mutableListOf<Collection<String>>()
        val deleteResult = mockk<DeleteTopicsResult>()
        every { adminClient.deleteTopics(capture(seenBatches)) } returns deleteResult
        every { deleteResult.all() } returns KafkaFuture.completedFuture(null)

        topicCleanupService(components, batchSize = 2).cleanup(adminClient)

        assertThat(seenBatches).hasSize(3)
        assertThat(seenBatches.map { it.size }).containsExactly(2, 2, 1)
        assertThat(seenBatches.flatten().toSet()).isEqualTo(orphans)
    }

    @Test
    fun `cleanup makes a single delete call when orphans fit within one batch`() {
        val components = listOf(ComponentConfig("utdanning", "elev", listOf("afk-no")))
        val orphans = setOf(
            "afk-no.fint-core.entity.utdanning-old-1",
            "afk-no.fint-core.entity.utdanning-old-2",
        )
        stubListTopics(orphans + "afk-no.fint-core.entity.utdanning-elev")
        stubDeleteTopics()

        topicCleanupService(components, batchSize = 10).cleanup(adminClient)

        verify(exactly = 1) { adminClient.deleteTopics(any<Collection<String>>()) }
    }
}
