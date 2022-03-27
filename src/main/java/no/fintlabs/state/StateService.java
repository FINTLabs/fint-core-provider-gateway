package no.fintlabs.state;

import no.fintlabs.adapter.models.FullSyncPage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
public class StateService {

    private final StateRepository stateRepository;

    public StateService(StateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    public void validate(FullSyncPage.Metadata metadata) {

        metadataValuesIsValide(metadata);
        ifWeAreInTheMidleOfASyncWeShouldHaveAState(metadata);
        pageAlreadySent(metadata);

        if (stateExists(metadata.getCorrId())) {
            syncIsFinished(metadata.getCorrId());
            pageOrderIsImproper(metadata);
        }
    }

    private void metadataValuesIsValide(FullSyncPage.Metadata metadata) {
        pageCannotBeGreaterThanTotalPage(metadata);
        pageCannotBeZero(metadata);
        totalPageCannotBeZero(metadata);
        totalSizeCannotBeZero(metadata);
        corrIdCannotBeEmpty(metadata);
    }

    private boolean stateExists(String corrId) {
        return stateRepository.has(corrId);
    }

    private void pageAlreadySent(FullSyncPage.Metadata metadata) {
        stateRepository.get(metadata.getCorrId())
                .stream()
                .filter(md -> metadata.getPage() == md.getPage())
                .findAny()
                .ifPresent(md -> {
                    throw new SyncStateAlreadyExists("Page already sent!");
                });
    }

    private void corrIdCannotBeEmpty(FullSyncPage.Metadata metadata) {
        if (!StringUtils.hasText(metadata.getCorrId())) {
            throw new IllegalArgumentException("corrId cannot be empty");
        }
    }

    private void totalSizeCannotBeZero(FullSyncPage.Metadata metadata) {
        if (metadata.getTotalSize() == 0) {
            throw new IllegalArgumentException("Total size cannot be 0");
        }
    }

    private void totalPageCannotBeZero(FullSyncPage.Metadata metadata) {
        if (metadata.getTotalPages() == 0) {
            throw new IllegalArgumentException("Total page cannot be 0");
        }
    }

    private void pageCannotBeZero(FullSyncPage.Metadata metadata) {
        if (metadata.getPage() == 0) {
            throw new IllegalArgumentException("Page cannot be 0");
        }
    }

    private void pageCannotBeGreaterThanTotalPage(FullSyncPage.Metadata metadata) {
        if (metadata.getPage() > metadata.getTotalPages()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Page (%s) cannot be greater than total pages (%s)",
                            metadata.getPage(),
                            metadata.getTotalPages()
                    )
            );
        }
    }

    private void ifWeAreInTheMidleOfASyncWeShouldHaveAState(FullSyncPage.Metadata metadata) {
        if (metadata.getPage() > 1) {
            if (!stateRepository.has(metadata.getCorrId())) {
                throw new SyncStateNotFound();
            }
        }
    }

    private void pageOrderIsImproper(FullSyncPage.Metadata metadata) {
        FullSyncPage.Metadata lastState = getLastState(metadata.getCorrId());
        if (metadata.getPage() <= lastState.getPage()) {
            throw new ImproperPageOrder();
        }
    }

    private void syncIsFinished(String corrId) {
        FullSyncPage.Metadata lastState = getLastState(corrId);

        if (lastState.getPage() == lastState.getTotalPages()) {
            throw new SyncStateGone();
        }
    }

    private List<FullSyncPage.Metadata> getSortedState(String corrId) {
        List<FullSyncPage.Metadata> metadata = stateRepository.get(corrId);

        return metadata.stream()
                .sorted(Comparator.comparingLong(FullSyncPage.Metadata::getPage))
                .toList();
    }

    private FullSyncPage.Metadata getLastState(String corrId) {
        List<FullSyncPage.Metadata> sortedState = getSortedState(corrId);

        return sortedState.get(sortedState.size() - 1);
    }
}
