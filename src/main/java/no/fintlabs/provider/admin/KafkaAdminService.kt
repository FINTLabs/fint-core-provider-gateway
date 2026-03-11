package no.fintlabs.provider.admin

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewPartitions
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.stereotype.Service

@Service
class KafkaAdminService(
    private val kafkaAdmin: KafkaAdmin
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaAdminService::class.java)
        private const val FINT_CORE_ENTITY = "fint-core.entity"
    }


    private fun <T> withAdminClient(block: (AdminClient) -> T): T {
        val properties = kafkaAdmin.configurationProperties
        return AdminClient.create(properties).use { client -> block(client) }
    }

    fun getEntityTopics(): List<String> = withAdminClient { client ->
        client.listTopics().names().get()
            .filter { it.contains(FINT_CORE_ENTITY) }
            .sorted()
    }

    fun getTopicPartitionCount(topic: String): Int = withAdminClient { client ->
        val description = client.describeTopics(listOf(topic)).allTopicNames().get()
        description[topic]?.partitions()?.size ?: 0
    }

    fun rebalanceEntityTopics(): RebalanceResult = withAdminClient { client ->
        val topics = client.listTopics().names().get()
            .filter { it.contains("fint-core.entity") }
            .sorted()

        if (topics.isEmpty()) {
            return@withAdminClient RebalanceResult(
                topics = emptyList(),
                consumerGroupsCleared = emptyList(),
                message = "No topics matching 'fint-core.entity' found"
            )
        }

        val consumerGroups = client.listConsumerGroups().all().get()
            .map { it.groupId() }

        val affectedGroups = mutableListOf<String>()
        for (groupId in consumerGroups) {
            val offsets = client.listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata().get()

            val hasEntityAssignment = offsets.keys.any { tp ->
                topics.contains(tp.topic())
            }

            if (hasEntityAssignment) {
                affectedGroups.add(groupId)
            }
        }

        for (groupId in affectedGroups) {
            try {
                val offsets = client.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata().get()

                val entityOffsets = offsets.filter { (tp, _) -> topics.contains(tp.topic()) }

                if (entityOffsets.isNotEmpty()) {
                    client.alterConsumerGroupOffsets(groupId, entityOffsets).all().get()
                    logger.info("Reset offsets for consumer group '{}' on {} entity partitions", groupId, entityOffsets.size)
                }
            } catch (e: Exception) {
                logger.warn("Could not reset offsets for consumer group '{}': {}", groupId, e.message)
            }
        }

        logger.info("Rebalance triggered for {} entity topics, {} consumer groups affected", topics.size, affectedGroups.size)

        RebalanceResult(
            topics = topics,
            consumerGroupsCleared = affectedGroups,
            message = "Rebalance triggered for ${topics.size} entity topics"
        )
    }

    fun updateEntityTopicPartitions(newPartitionCount: Int): PartitionUpdateResult = withAdminClient { client ->
        require(newPartitionCount > 0) { "Partition count must be greater than 0" }

        val topics = client.listTopics().names().get()
            .filter { it.contains("fint-core.entity") }
            .sorted()

        if (topics.isEmpty()) {
            return@withAdminClient PartitionUpdateResult(
                updated = emptyList(),
                skipped = emptyList(),
                failed = emptyList(),
                message = "No topics matching 'fint-core.entity' found"
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

        if (toUpdate.isNotEmpty()) {
            val results = client.createPartitions(toUpdate)
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
