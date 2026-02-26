package no.fintlabs.provider.kafka;

public final class TopicNamesConstants {

    private TopicNamesConstants() {
    }

    public static final String TOPIC_RETENTION_TIME = "topic-retention-time";
    public static final String LAST_MODIEFIED = "entity-retention-time"; // TODO: Change this to last-modified
    public static final String SYNC_CORRELATION_ID = "sync-correlation-id";
    public static final String SYNC_TOTAL_SIZE = "sync-total-size";
    public static final String SYNC_TYPE = "sync-type";

    public static final String HEARTBEAT_EVENT_NAME = "adapter-health";
    public static final String ADAPTER_REGISTER_EVENT_NAME = "adapter-register";
    public static final String ADAPTER_FULL_SYNC_EVENT_NAME = "adapter-full-sync";
    public static final String ADAPTER_DELTA_SYNC_EVENT_NAME = "adapter-delta-sync";
    public static final String ADAPTER_DELETE_SYNC_EVENT_NAME = "adapter-delete-sync";

}
