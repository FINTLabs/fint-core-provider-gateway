package no.fintlabs.event

import no.fintlabs.adapter.models.RequestFintEvent
import no.fintlabs.event.request.RequestEventService
import spock.lang.Specification

class RequestEventServiceSpec extends Specification {

    private final String ORG_ID = "vigoiks.no";

    private RequestEventService eventService;

    void setup() {
        eventService = new RequestEventService()
    }

    def "Test getEvents"() {
        given:
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("okonomi", "kodeverk", "vare"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("okonomi", "kodeverk", "vare"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravarsoversikt"));

        when:
        def result = eventService.getEvents(ORG_ID, "", "", "", 0)

        then:
        result.size() == 5
    }

    def "Test getEvents with org filter"() {
        given:
        eventService.addEvent(createEvent("test.no", "utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("vigoiks.no", "okonomi", "kodeverk", "vare"));
        eventService.addEvent(createEvent("test.no", "utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("test.no", "okonomi", "kodeverk", "vare"));
        eventService.addEvent(createEvent("vigoiks.no", "utdanning", "vurdering", "fravarsoversikt"));

        when:
        def result = eventService.getEvents("vigoiks.no", "", "", "", 0)

        then:
        result.size() == 2
    }

    def "Test getEvents with domain-package-resource filter"() {
        given:
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("okonomi", "kodeverk", "vare"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("okonomi", "kodeverk", "vare"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravarsoversikt"));

        when:
        def result = eventService.getEvents(ORG_ID, "utdanning", "vurdering", "fravar", 0)

        then:
        result.size() == 2
    }

    def "Test getEvents with size limit"() {
        given:
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));

        when:
        def result = eventService.getEvents(ORG_ID, "utdanning", "vurdering", "fravar", 3)

        then:
        result.size() == 3
    }

    private RequestFintEvent createEvent(String domainName, String packageName, String resourceName) {
        return createEvent(ORG_ID, domainName, packageName, resourceName);
    }

    private RequestFintEvent createEvent(String orgId, String domainName, String packageName, String resourceName) {
        return RequestFintEvent.builder()
                .corrId(UUID.randomUUID().toString())
                .orgId(orgId)
                .domainName(domainName)
                .packageName(packageName)
                .resourceName(resourceName)
                .build();
    }
}
