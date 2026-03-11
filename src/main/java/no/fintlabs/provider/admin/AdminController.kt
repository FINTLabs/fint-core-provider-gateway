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
        @AuthenticationPrincipal corePrincipal: CorePrincipal
    ): ResponseEntity<List<String>> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.getEntityTopics())
    }

    @PostMapping("entity-topics/rebalance")
    fun rebalanceEntityTopics(
        @AuthenticationPrincipal corePrincipal: CorePrincipal
    ): ResponseEntity<RebalanceResult> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.rebalanceEntityTopics())
    }

    @PutMapping("entity-topics/partitions")
    fun updateEntityTopicPartitions(
        @AuthenticationPrincipal corePrincipal: CorePrincipal,
        @RequestParam partitions: Int
    ): ResponseEntity<PartitionUpdateResult> {
        validateAdminAccess(corePrincipal)
        return ResponseEntity.ok(kafkaAdminService.updateEntityTopicPartitions(partitions))
    }

    @GetMapping("entity-topics/{topic}/partitions")
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
