package no.fintlabs.provider.config

import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class KafkaConfig(
    @Value($$"${spring.kafka.consumer.group-id:}") groupId: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    val groupIdSuffix: String = createGroupIdSuffix(groupId)

    private fun createGroupIdSuffix(groupId: String): String {
        val suffix = RandomStringUtils.secure().nextAlphanumeric(SUFFIX_LENGTH).lowercase()
        logger.info("Group-id: $groupId-$suffix")
        return suffix
    }

    companion object {
        const val SUFFIX_LENGTH: Int = 8
    }
}
