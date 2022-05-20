package no.fintlabs.state;

import no.fintlabs.adapter.models.FullSyncPage;
import no.fintlabs.adapter.models.SyncPageMetadata;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class StateRepository {

    private Map<String, List<SyncPageMetadata>> state;

    public StateRepository() {
        state = new HashMap<>();
    }

    public boolean has(String corrId) {
        return state.containsKey(corrId);
    }

    public void update(SyncPageMetadata metadata) {
        List<SyncPageMetadata> states = state.get(metadata.getCorrId());
        states.add(metadata);
    }

    public void add(SyncPageMetadata metadata) {
        state.put(metadata.getCorrId(), Collections.singletonList(metadata));
    }

    public List<SyncPageMetadata> get(String corrId) {
        return Optional.ofNullable(state.get(corrId)).orElse(Collections.emptyList());
    }

}
