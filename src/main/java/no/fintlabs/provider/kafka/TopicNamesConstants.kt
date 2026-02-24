package no.fintlabs.provider.kafka

object TopicNamesConstants {
    const val FINTLABS_NO = "fintlabs-no"
    const val FINT_CORE = "fint-core"
    const val LAST_UPDATED = "entity-retention-time" // TODO: Change this to last-modified
    const val SYNC_CORRELATION_ID = "sync-correlation-id"
    const val SYNC_TOTAL_SIZE = "sync-total-size"
    const val SYNC_TYPE = "sync-type"

    const val HEARTBEAT_EVENT_NAME = "adapter-health"
    const val ADAPTER_REGISTER_EVENT_NAME = "adapter-register"
    const val ADAPTER_FULL_SYNC_EVENT_NAME = "adapter-full-sync"
    const val ADAPTER_DELTA_SYNC_EVENT_NAME = "adapter-delta-sync"
    const val ADAPTER_DELETE_SYNC_EVENT_NAME = "adapter-delete-sync"
}