package no.fintlabs.state;

import no.fintlabs.adapter.models.SyncPageMetadata;
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

    public void validate(SyncPageMetadata metadata) {

        metadataValuesIsValide(metadata);
        ifWeAreInTheMidleOfASyncWeShouldHaveAState(metadata);
        pageAlreadySent(metadata);

        if (stateExists(metadata.getCorrId())) {
            syncIsFinished(metadata.getCorrId());
            pageOrderIsImproper(metadata);
        }
    }

    private void metadataValuesIsValide(SyncPageMetadata metadata) {
        pageCannotBeGreaterThanTotalPage(metadata);
        pageCannotBeZero(metadata);
        totalPageCannotBeZero(metadata);
        totalSizeCannotBeZero(metadata);
        corrIdCannotBeEmpty(metadata);
    }

    private boolean stateExists(String corrId) {
        return stateRepository.has(corrId);
    }

    private void pageAlreadySent(SyncPageMetadata metadata) {
        stateRepository.get(metadata.getCorrId())
                .stream()
                .filter(md -> metadata.getPage() == md.getPage())
                .findAny()
                .ifPresent(md -> {
                    throw new SyncStateAlreadyExists("Page already sent!");
                });
    }

    private void corrIdCannotBeEmpty(SyncPageMetadata metadata) {
        if (!StringUtils.hasText(metadata.getCorrId())) {
            throw new IllegalArgumentException("corrId cannot be empty");
        }
    }

    private void totalSizeCannotBeZero(SyncPageMetadata metadata) {
        if (metadata.getTotalSize() == 0) {
            throw new IllegalArgumentException("Total size cannot be 0");
        }
    }

    private void totalPageCannotBeZero(SyncPageMetadata metadata) {
        if (metadata.getTotalPages() == 0) {
            throw new IllegalArgumentException("Total page cannot be 0");
        }
    }

    private void pageCannotBeZero(SyncPageMetadata metadata) {
        if (metadata.getPage() == 0) {
            throw new IllegalArgumentException("Page cannot be 0");
        }
    }

    private void pageCannotBeGreaterThanTotalPage(SyncPageMetadata metadata) {
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

    private void ifWeAreInTheMidleOfASyncWeShouldHaveAState(SyncPageMetadata metadata) {
        if (metadata.getPage() > 1) {
            if (!stateRepository.has(metadata.getCorrId())) {
                throw new SyncStateNotFound();
            }
        }
    }

    private void pageOrderIsImproper(SyncPageMetadata metadata) {
        SyncPageMetadata lastState = getLastState(metadata.getCorrId());
        if (metadata.getPage() <= lastState.getPage()) {
            throw new ImproperPageOrder();
        }
    }

    private void syncIsFinished(String corrId) {
        SyncPageMetadata lastState = getLastState(corrId);

        if (lastState.getPage() == lastState.getTotalPages()) {
            throw new SyncStateGone();
        }
    }

    private List<SyncPageMetadata> getSortedState(String corrId) {
        List<SyncPageMetadata> metadata = stateRepository.get(corrId);

        return metadata.stream()
                .sorted(Comparator.comparingLong(SyncPageMetadata::getPage))
                .toList();
    }

    private SyncPageMetadata getLastState(String corrId) {
        List<SyncPageMetadata> sortedState = getSortedState(corrId);

        return sortedState.get(sortedState.size() - 1);
    }
}
