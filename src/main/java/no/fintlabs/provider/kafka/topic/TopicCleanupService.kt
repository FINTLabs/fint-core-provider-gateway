package no.fintlabs.provider.kafka.topic

import no.fintlabs.provider.config.CleanupTopicsProperties
import no.fintlabs.provider.config.ProviderProperties
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.ADAPTER_DELETE_SYNC_EVENT_NAME
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.ADAPTER_DELTA_SYNC_EVENT_NAME
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.ADAPTER_FULL_SYNC_EVENT_NAME
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.ADAPTER_REGISTER_EVENT_NAME
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.CONSUMER_ERROR_EVENT_NAME
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.FINT_CORE
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.HEARTBEAT_EVENT_NAME
import no.fintlabs.provider.kafka.topic.TopicNamesConstants.SYNC_STATUS_EVENT_NAME
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.EventTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import no.novari.kafka.topic.name.TopicNameService
import org.apache.kafka.clients.admin.AdminClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConditionalOnProperty(prefix = "fint.provider.cleanup-topics", name = ["enabled"], havingValue = "true")
class TopicCleanupService(
    private val providerProperties: ProviderProperties,
    private val cleanupTopicsProperties: CleanupTopicsProperties,
    private val topicNameService: TopicNameService,
    private val kafkaAdmin: KafkaAdmin,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Startup hook: opens an [AdminClient] against the configured Kafka cluster and runs
     * a single cleanup pass, closing the client when the pass finishes.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun cleanupOnStartup() {
        AdminClient.create(kafkaAdmin.configurationProperties).use { cleanup(it) }
    }

    /**
     * Runs one cleanup pass against [adminClient]:
     *  1. Returns early when no org-ids are configured — cleanup is opt-in per org.
     *  2. Builds the "keep" set via [computeExpectedTopicNames].
     *  3. Lists every topic currently on the broker.
     *  4. Collects orphan topics per configured org via [collectOrphansToDelete].
     *  5. If any orphans are found, deletes them in paced batches.
     *
     * @return the set of topics that were actually deleted (empty if there was nothing to do).
     */
    fun cleanup(adminClient: AdminClient): Set<String> {
        if (cleanupTopicsProperties.orgIds.isEmpty()) {
            logger.info("No org-ids configured under 'fint.provider.cleanup-topics' — nothing to do")
            return emptySet()
        }

        val expected = computeExpectedTopicNames()
        val existing = adminClient.listTopics().names().get()

        val toDelete = collectOrphansToDelete(existing, expected)
        if (toDelete.isEmpty()) return emptySet()

        deleteInBatches(adminClient, toDelete)
        return toDelete
    }

    /**
     * Walks each configured org and gathers the topics that should be removed. A topic is
     * considered an orphan when it starts with `<orgId>.`, contains `fint-core`, and is
     * absent from [expected]. Non-fint-core topics and topics belonging to orgs that are
     * not in the cleanup config are never included.
     */
    private fun collectOrphansToDelete(existing: Set<String>, expected: Set<String>): Set<String> {
        val toDelete = mutableSetOf<String>()
        cleanupTopicsProperties.orgIds.forEach { orgId ->
            val orphansForOrg = findOrphans(existing, orgId, expected)
            if (orphansForOrg.isEmpty()) {
                logger.info("No obsolete '{}' topics for org '{}'", FINT_CORE, orgId)
            } else {
                logger.warn(
                    "Org '{}': deleting {} obsolete topic(s): {}",
                    orgId, orphansForOrg.size, orphansForOrg
                )
                toDelete += orphansForOrg
            }
        }
        return toDelete
    }

    /**
     * Filters [existing] to topics that belong to [orgId] (exact `<orgId>.` prefix), contain
     * `fint-core`, and are not in the [expected] keep-set.
     */
    private fun findOrphans(existing: Set<String>, orgId: String, expected: Set<String>): Set<String> =
        existing
            .filter { it.startsWith("$orgId.") && it.contains(FINT_CORE) }
            .filterNot(expected::contains)
            .toSet()

    /**
     * Splits [toDelete] into chunks of `batchSize` and issues one `deleteTopics` call per
     * chunk, pausing [CleanupTopicsProperties.batchDelay] between chunks so the broker is
     * never overwhelmed. No pause follows the final batch.
     */
    private fun deleteInBatches(adminClient: AdminClient, toDelete: Set<String>) {
        val batchSize = cleanupTopicsProperties.batchSize.coerceAtLeast(1)
        val batches = toDelete.chunked(batchSize)
        val total = toDelete.size
        var deleted = 0
        batches.forEachIndexed { index, batch ->
            adminClient.deleteTopics(batch).all().get()
            deleted += batch.size
            logger.info(
                "Batch {}/{} done — {}/{} topic(s) deleted",
                index + 1, batches.size, deleted, total
            )
            if (index < batches.lastIndex) pauseBetweenBatches()
        }
    }

    private fun pauseBetweenBatches() {
        val delay = cleanupTopicsProperties.batchDelay
        if (delay > Duration.ZERO) Thread.sleep(delay.toMillis())
    }

    /**
     * Builds the full set of topic names the gateway is expected to own. These topics are
     * never deleted by the cleanup pass, even when they match the `fint-core` filter.
     *
     * The set contains, for every `(component, orgId)` pair in `components.yaml`:
     *  - the entity topic (`<orgId>.fint-core.entity.<domain>-<package>`)
     *  - the request event topic (`<orgId>.fint-core.event.<domain>-<package>-request`)
     *  - the response event topic (`<orgId>.fint-core.event.<domain>-<package>-response`)
     *  - the relation-update topic — only when the component has `relation-update: true`
     *
     * It also contains the seven global event topics under the application-default orgId:
     * `adapter-health`, `adapter-register`, `adapter-full-sync`, `adapter-delta-sync`,
     * `adapter-delete-sync`, `consumer-error`, `sync-status`.
     */
    fun computeExpectedTopicNames(): Set<String> {
        val expected = mutableSetOf<String>()

        providerProperties.components.forEach { component ->
            val componentName = "${component.domainName}-${component.packageName}"
            component.orgIds.forEach { orgId ->
                expected += entityTopicName(orgId, componentName) // Resource entity topic
                expected += eventTopicName(orgId, "$componentName-request") // Event request topic
                expected += eventTopicName(orgId, "$componentName-response") // Event response topic
                if (component.relationUpdate) {
                    expected += entityTopicName(orgId, "$componentName-relation-update")
                }
            }
        }

        listOf(
            HEARTBEAT_EVENT_NAME,
            ADAPTER_REGISTER_EVENT_NAME,
            ADAPTER_FULL_SYNC_EVENT_NAME,
            ADAPTER_DELTA_SYNC_EVENT_NAME,
            ADAPTER_DELETE_SYNC_EVENT_NAME,
            CONSUMER_ERROR_EVENT_NAME,
            SYNC_STATUS_EVENT_NAME,
        ).forEach { eventName ->
            expected += topicNameService.validateAndMapToTopicName(
                EventTopicNameParameters.builder()
                    .topicNamePrefixParameters(
                        TopicNamePrefixParameters.stepBuilder()
                            .orgIdApplicationDefault()
                            .domainContextApplicationDefault()
                            .build()
                    )
                    .eventName(eventName)
                    .build()
            )
        }

        return expected
    }

    private fun entityTopicName(orgId: String, resourceName: String): String =
        topicNameService.validateAndMapToTopicName(
            EntityTopicNameParameters.builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters.stepBuilder()
                        .orgId(orgId)
                        .domainContextApplicationDefault()
                        .build()
                )
                .resourceName(resourceName)
                .build()
        )

    private fun eventTopicName(orgId: String, eventName: String): String =
        topicNameService.validateAndMapToTopicName(
            EventTopicNameParameters.builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters.stepBuilder()
                        .orgId(orgId)
                        .domainContextApplicationDefault()
                        .build()
                )
                .eventName(eventName)
                .build()
        )
}
