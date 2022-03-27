package no.fintlabs.state

import no.fintlabs.adapter.models.FullSyncPage
import spock.lang.Specification

class StateServiceSpec extends Specification {

    StateService stateService
    String corrId
    StateRepository repository

    void setup() {
        this.corrId = UUID.randomUUID().toString()
        repository = new StateRepository(state: TestStateFactory.createState(this.corrId))
        stateService = new StateService(repository)
    }

    def "If page is 0 it should cause an IllegalArgumentException"() {
        given:
        def metadata = FullSyncPage.Metadata.builder()
                .corrId(UUID.randomUUID().toString())
                .totalSize(20)
                .totalPages(2)
                .build()

        when:
        stateService.validate(metadata)

        then:
        thrown(IllegalArgumentException)
    }

    def "If corrId is null it should cause an IllegalArgumentException"() {
        given:
        def metadata = FullSyncPage.Metadata.builder()
                .totalSize(20)
                .totalPages(2)
                .page(1)
                .build()

        when:
        stateService.validate(metadata)

        then:
        thrown(IllegalArgumentException)
    }

    def "If total page is 0 it should cause an IllegalArgumentException"() {
        given:
        def metadata = FullSyncPage.Metadata.builder()
                .corrId(UUID.randomUUID().toString())
                .totalSize(20)
                .page(1)
                .build()

        when:
        stateService.validate(metadata)

        then:
        thrown(IllegalArgumentException)
    }

    def "If total size is 0 it should cause an IllegalArgumentException"() {
        given:
        def metadata = FullSyncPage.Metadata.builder()
                .corrId(UUID.randomUUID().toString())
                .totalPages(3)
                .page(1)
                .build()

        when:
        stateService.validate(metadata)

        then:
        thrown(IllegalArgumentException)
    }

    def "If page is greater than total pages it should cause an IllegalArgumentException"() {
        given:
        def metadata = FullSyncPage.Metadata.builder()
                .corrId(UUID.randomUUID().toString())
                .totalPages(3)
                .totalSize(30)
                .page(4)
                .build()

        when:
        stateService.validate(metadata)

        then:
        thrown(IllegalArgumentException)
    }

    def "Metadata with page == 1 and an unknown corrId should be considered a new full sync and not cause any exceptions."() {
        given:
        def metadata = FullSyncPage.Metadata.builder()
                .corrId(UUID.randomUUID().toString())
                .totalSize(20)
                .totalPages(2)
                .page(1)
                .build()

        when:
        stateService.validate(metadata)

        then:
        noExceptionThrown()
    }

    def "If a corrId with the page number already exists it should cause a SyncStateAlreadyExists"() {
        given:
        def metadata = FullSyncPage.Metadata.builder()
                .corrId(corrId)
                .totalSize(30)
                .totalPages(3)
                .page(1)
                .build()
        when:
        stateService.validate(metadata)

        then:
        thrown(SyncStateAlreadyExists)
    }

    def "Validating metadata with a unknown corrId and page > 1 should result in a SyncStateNotFound exception"() {
        given:
        def metadata = FullSyncPage.Metadata.builder()
                .corrId(UUID.randomUUID().toString())
                .totalSize(20)
                .totalPages(2)
                .page(2)
                .build()
        when:
        stateService.validate(metadata)

        then:
        thrown(SyncStateNotFound)
    }

    def "Validating metadata when the full sync is finished should result in a SyncStateGone exception"() {
        given:
        def metadata = FullSyncPage.Metadata.builder()
                .corrId(this.corrId)
                .build()
        when:
        stateService.validate(metadata)

        then:
        thrown(SyncStateGone)
    }
}
