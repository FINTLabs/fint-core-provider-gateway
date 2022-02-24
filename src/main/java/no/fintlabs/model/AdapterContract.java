package no.fintlabs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdapterContract {
    private String id;
    private String orgId;
    private String username;
    private long pingIntervalInMs;
    private List<AdapterCapability> capability;
    private long time;

    @Data
    @Builder
    public static class AdapterCapability {
        private String domain;
        @JsonProperty("package")
        private String packageName;
        @JsonProperty("class")
        private String clazz;
        private int fullSyncIntervalInDays;
        private DeltaSyncInterval deltaSyncInterval;

        public String getEntityUri() {
            return String.format("/%s/%s/%s", domain, packageName, clazz);
        }

        public String getComponent() {
            return String.format("%s-%s", domain, packageName);
        }

        public enum DeltaSyncInterval {
            /**
             * This indicates that the adapter will send updates as soon as they are availiable in the application.
             */
            IMMEDIATE,
            /**
             * This indicates that the adapter will send updates every <=15 minutes.
             */
            LEGACY
        }
    }
}
