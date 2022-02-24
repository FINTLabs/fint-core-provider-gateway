package no.fintlabs.model;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class FullSyncEntity {

    private Metadata metadata;
    private List<HashMap<String, ?>> resources;

    @Data
    @Builder
    public static class Metadata {
        private String corrId;
        private String orgId;
        private long totalSize;
        private long page;
        private long totalPages;
    }
}
