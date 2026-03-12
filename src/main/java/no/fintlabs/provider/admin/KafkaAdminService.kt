package no.fintlabs.provider.admin

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewPartitions
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.stereotype.Service

enum class TopicType(val identifier: String) {
    ENTITY("fint-core.entity"),
    EVENT("fint-core.event");
}

@Service
class KafkaAdminService(
    private val kafkaAdmin: KafkaAdmin
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaAdminService::class.java)
        private const val BATCH_SIZE = 20
        private const val ADMIN_REQUEST_TIMEOUT_MS = 120_000
    }

    private fun <T> withAdminClient(block: (AdminClient) -> T): T {
        val properties = HashMap(kafkaAdmin.configurationProperties)
        properties[AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG] = ADMIN_REQUEST_TIMEOUT_MS
        properties[AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG] = ADMIN_REQUEST_TIMEOUT_MS
        return AdminClient.create(properties).use { client -> block(client) }
    }

    private fun buildPrefix(org: String, type: TopicType, pattern: String?): String {
        val base = "${org}.${type.identifier}."
        return if (pattern != null) "$base$pattern" else base
    }

    private fun findTopics(client: AdminClient, type: TopicType, org: String?, pattern: String?): List<String> =
        client.listTopics().names().get()
            .filter { it.contains(type.identifier) }
            .filter { topic ->
                if (org == null) true
                else topic.startsWith(buildPrefix(org, type, pattern))
            }
            .sorted()

    fun getTopics(type: TopicType, org: String?, pattern: String?): List<String> = withAdminClient { client ->
        findTopics(client, type, org, pattern)
    }

    fun getTopicPartitionCount(topic: String): Int = withAdminClient { client ->
        val description = client.describeTopics(listOf(topic)).allTopicNames().get()
        description[topic]?.partitions()?.size ?: 0
    }

    fun rebalanceTopics(type: TopicType, org: String, pattern: String?): RebalanceResult = withAdminClient { client ->
        val prefix = buildPrefix(org, type, pattern)
        val topics = findTopics(client, type, org, pattern)

        logger.info("Rebalancing {} topics with prefix '{}', found {} topics", type, prefix, topics.size)

        if (topics.isEmpty()) {
            return@withAdminClient RebalanceResult(
                topics = emptyList(),
                consumerGroupsCleared = emptyList(),
                message = "No ${type.name.lowercase()} topics matching prefix '$prefix'"
            )
        }

        val topicSet = topics.toSet()

        val consumerGroups = client.listConsumerGroups().all().get()
            .map { it.groupId() }

        val affectedGroups = mutableListOf<String>()
        for (groupId in consumerGroups) {
            val offsets = client.listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata().get()

            val hasAssignment = offsets.keys.any { tp -> topicSet.contains(tp.topic()) }

            if (hasAssignment) {
                affectedGroups.add(groupId)
            }
        }

        for (groupId in affectedGroups) {
            try {
                val offsets = client.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata().get()

                val matchingOffsets = offsets.filter { (tp, _) -> topicSet.contains(tp.topic()) }

                if (matchingOffsets.isNotEmpty()) {
                    client.alterConsumerGroupOffsets(groupId, matchingOffsets).all().get()
                    logger.info("Reset offsets for consumer group '{}' on {} partitions", groupId, matchingOffsets.size)
                }
            } catch (e: Exception) {
                logger.warn("Could not reset offsets for consumer group '{}': {}", groupId, e.message)
            }
        }

        logger.info("Rebalance triggered for {} {} topics, {} consumer groups affected", topics.size, type, affectedGroups.size)

        RebalanceResult(
            topics = topics,
            consumerGroupsCleared = affectedGroups,
            message = "Rebalance triggered for ${topics.size} ${type.name.lowercase()} topics with prefix '$prefix'"
        )
    }

    fun updateTopicPartitions(type: TopicType, org: String, pattern: String?, newPartitionCount: Int): PartitionUpdateResult = withAdminClient { client ->
        require(newPartitionCount > 0) { "Partition count must be greater than 0" }

        val prefix = buildPrefix(org, type, pattern)
        val topics = findTopics(client, type, org, pattern)

        logger.info("Updating partitions for {} topics with prefix '{}', found {} topics", type, prefix, topics.size)

        if (topics.isEmpty()) {
            return@withAdminClient PartitionUpdateResult(
                updated = emptyList(),
                skipped = emptyList(),
                failed = emptyList(),
                message = "No ${type.name.lowercase()} topics matching prefix '$prefix'"
            )
        }

        val descriptions = client.describeTopics(topics).allTopicNames().get()

        val toUpdate = mutableMapOf<String, NewPartitions>()
        val skipped = mutableListOf<TopicPartitionInfo>()

        for ((topicName, description) in descriptions) {
            val currentCount = description.partitions().size
            if (currentCount >= newPartitionCount) {
                skipped.add(TopicPartitionInfo(topicName, currentCount, newPartitionCount))
            } else {
                toUpdate[topicName] = NewPartitions.increaseTo(newPartitionCount)
            }
        }

        val updated = mutableListOf<TopicPartitionInfo>()
        val failed = mutableListOf<TopicPartitionFailure>()

        logger.info("Updating partitions to {} for {} topics ({} skipped, already >= {})", newPartitionCount, toUpdate.size, skipped.size, newPartitionCount)

        toUpdate.entries.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            val batchMap = batch.associate { it.key to it.value }
            logger.info("Processing batch {}/{} ({} topics)", batchIndex + 1, (toUpdate.size + BATCH_SIZE - 1) / BATCH_SIZE, batchMap.size)

            val results = client.createPartitions(batchMap)
            for ((topicName, future) in results.values()) {
                val currentCount = descriptions[topicName]?.partitions()?.size ?: 0
                try {
                    future.get()
                    updated.add(TopicPartitionInfo(topicName, currentCount, newPartitionCount))
                    logger.info("Updated partitions for '{}': {} -> {}", topicName, currentCount, newPartitionCount)
                } catch (e: Exception) {
                    failed.add(TopicPartitionFailure(topicName, currentCount, newPartitionCount, e.message ?: "Unknown error"))
                    logger.error("Failed to update partitions for '{}': {}", topicName, e.message)
                }
            }
        }

        PartitionUpdateResult(
            updated = updated,
            skipped = skipped,
            failed = failed,
            message = "Updated ${updated.size} topics, skipped ${skipped.size}, failed ${failed.size}"
        )
    }

    fun deleteTopics(type: TopicType, org: String, pattern: String?): DeleteResult = withAdminClient { client ->
        val prefix = buildPrefix(org, type, pattern)
        val topics = findTopics(client, type, org, pattern)

        logger.info("Deleting {} topics with prefix '{}', found {} topics", type, prefix, topics.size)

        if (topics.isEmpty()) {
            return@withAdminClient DeleteResult(
                deleted = emptyList(),
                failed = emptyList(),
                message = "No ${type.name.lowercase()} topics matching prefix '$prefix'"
            )
        }

        val deleted = mutableListOf<String>()
        val failed = mutableListOf<TopicDeleteFailure>()

        topics.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            logger.info("Deleting batch {}/{} ({} topics)", batchIndex + 1, (topics.size + BATCH_SIZE - 1) / BATCH_SIZE, batch.size)

            val results = client.deleteTopics(batch)
            for (topicName in batch) {
                try {
                    results.topicNameValues()[topicName]?.get()
                    deleted.add(topicName)
                    logger.info("Deleted topic '{}'", topicName)
                } catch (e: Exception) {
                    failed.add(TopicDeleteFailure(topicName, e.message ?: "Unknown error"))
                    logger.error("Failed to delete topic '{}': {}", topicName, e.message)
                }
            }
        }

        logger.info("Deleted {} topics, {} failed", deleted.size, failed.size)

        DeleteResult(
            deleted = deleted,
            failed = failed,
            message = "Deleted ${deleted.size} topics, failed ${failed.size}"
        )
    }
}

data class RebalanceResult(
    val topics: List<String>,
    val consumerGroupsCleared: List<String>,
    val message: String
)

data class PartitionUpdateResult(
    val updated: List<TopicPartitionInfo>,
    val skipped: List<TopicPartitionInfo>,
    val failed: List<TopicPartitionFailure>,
    val message: String
)

data class TopicPartitionInfo(
    val topic: String,
    val previousPartitions: Int,
    val requestedPartitions: Int
)

data class TopicPartitionFailure(
    val topic: String,
    val previousPartitions: Int,
    val requestedPartitions: Int,
    val error: String
)

data class DeleteResult(
    val deleted: List<String>,
    val failed: List<TopicDeleteFailure>,
    val message: String
)

data class TopicDeleteFailure(
    val topic: String,
    val error: String
)
