package no.fintlabs.state;

import no.fintlabs.adapter.models.FullSyncPage;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class StateRepository {

    private Map<String, List<FullSyncPage.Metadata>> state;

    public StateRepository() {
        state = new HashMap<>();
    }

    public boolean has(String corrId) {
        return state.containsKey(corrId);
    }

    public void update(FullSyncPage.Metadata metadata) {
        List<FullSyncPage.Metadata> states = state.get(metadata.getCorrId());
        states.add(metadata);
    }

    public void add(FullSyncPage.Metadata metadata) {
        state.put(metadata.getCorrId(), Collections.singletonList(metadata));
    }

    public List<FullSyncPage.Metadata> get(String corrId) {
        return Optional.ofNullable(state.get(corrId)).orElse(Collections.emptyList());
    }

}
