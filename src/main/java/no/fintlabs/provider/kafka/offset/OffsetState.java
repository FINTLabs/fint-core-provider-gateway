package no.fintlabs.provider.kafka.offset;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class OffsetState {

    private volatile long requestOffset = -1L;
    private volatile long responseOffset = -1L;
    private volatile long contractOffset = -1L;

}
