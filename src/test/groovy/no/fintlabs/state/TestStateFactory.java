package no.fintlabs.state;

import no.fintlabs.adapter.models.FullSyncPage;

import java.util.*;
import java.util.stream.IntStream;

public class TestStateFactory {

    public static Map<String, List<FullSyncPage.Metadata>> createState(String corrId) {
        Map<String, List<FullSyncPage.Metadata>> state = new HashMap<>();
        List<FullSyncPage.Metadata> metadataList = new ArrayList<>();


        IntStream.range(0, 3).forEach(i -> metadataList.add(createMetadata(corrId, i)));
        state.put(corrId, metadataList);

        return state;
    }

    private static FullSyncPage.Metadata createMetadata(String corrId, long page) {
        return FullSyncPage.Metadata.builder()
                .orgId("test.no")
                .corrId(corrId)
                .page(page + 1)
                .totalPages(3)
                .totalSize(30)
                .build();
    }
}
