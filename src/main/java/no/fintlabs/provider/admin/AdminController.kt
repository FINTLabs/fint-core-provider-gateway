package no.fintlabs.provider.admin

import no.fintlabs.core.resource.server.security.authentication.CorePrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

private const val REQUIRED_ORG_ID = "fintlabs.no"

@RestController
@RequestMapping("admin")
class AdminController(
    private val kafkaAdminService: KafkaAdminService
) {

    private fun validateAdminAccess(corePrincipal: CorePrincipal) {
        if (corePrincipal.doesNotContainAsset(REQUIRED_ORG_ID)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access restricted to $REQUIRED_ORG_ID")
        }
    }

    @GetMapping("entity-topics")
    fun listEntityTopics(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestParam(required = false) org: String?,
        @RequestParam(required = false) pattern: String?
    ): ResponseEntity<List<String>> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.getTopics(TopicType.ENTITY, org, pattern))
    }

    @PostMapping("entity-topics/rebalance")
    fun rebalanceEntityTopics(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestParam org: String,
        @RequestParam(required = false) pattern: String?
    ): ResponseEntity<RebalanceResult> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.rebalanceTopics(TopicType.ENTITY, org, pattern))
    }

    @PutMapping("entity-topics/partitions")
    fun updateEntityTopicPartitions(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestParam org: String,
        @RequestParam partitions: Int,
        @RequestParam(required = false) pattern: String?
    ): ResponseEntity<PartitionUpdateResult> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.updateTopicPartitions(TopicType.ENTITY, org, pattern, partitions))
    }

    @DeleteMapping("entity-topics")
    fun deleteEntityTopics(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestParam org: String,
        @RequestParam(required = false) pattern: String?
    ): ResponseEntity<DeleteResult> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.deleteTopics(TopicType.ENTITY, org, pattern))
    }

    @GetMapping("event-topics")
    fun listEventTopics(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestParam(required = false) org: String?,
        @RequestParam(required = false) pattern: String?
    ): ResponseEntity<List<String>> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.getTopics(TopicType.EVENT, org, pattern))
    }

    @PostMapping("event-topics/rebalance")
    fun rebalanceEventTopics(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestParam org: String,
        @RequestParam(required = false) pattern: String?
    ): ResponseEntity<RebalanceResult> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.rebalanceTopics(TopicType.EVENT, org, pattern))
    }

    @PutMapping("event-topics/partitions")
    fun updateEventTopicPartitions(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestParam org: String,
        @RequestParam partitions: Int,
        @RequestParam(required = false) pattern: String?
    ): ResponseEntity<PartitionUpdateResult> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.updateTopicPartitions(TopicType.EVENT, org, pattern, partitions))
    }

    @DeleteMapping("event-topics")
    fun deleteEventTopics(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestParam org: String,
        @RequestParam(required = false) pattern: String?
    ): ResponseEntity<DeleteResult> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.deleteTopics(TopicType.EVENT, org, pattern))
    }

    @GetMapping("topics/{topic}/partitions")
    fun getTopicPartitions(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @PathVariable topic: String
    ): ResponseEntity<Map<String, Any>> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(
            mapOf(
                "topic" to topic,
                "partitions" to kafkaAdminService.getTopicPartitionCount(topic)
            )
        )
    }
}
