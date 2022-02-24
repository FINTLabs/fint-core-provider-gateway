package no.fintlabs.model;

import lombok.Builder;
import lombok.Data;
import javax.validation.constraints.*;

@Data
@Builder
public class AdapterPing {
    @NotNull
    private String adapterId;
    @NotNull
    private String username;
    @NotNull
    private String orgId;
//    @NotNull
//    private Status status;
    @NotNull
    private long time;

//    public enum Status {
//        APPLICATION_HEALTHY,
//        APPLICATION_UNHEALTHY
//    }
}
