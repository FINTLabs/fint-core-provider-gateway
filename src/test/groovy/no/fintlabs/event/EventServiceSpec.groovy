package no.fintlabs.event

import no.fintlabs.adapter.models.RequestFintEvent
import no.fintlabs.kafka.event.EventConsumerFactoryService
import spock.lang.Specification

class EventServiceSpec extends Specification {

    private final String ORG_ID = "vigoiks.no";

    def "Test getEvents"() {
        given:
        def events = new ArrayList<>();
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("okonomi", "kodeverk", "vare"));
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("okonomi", "kodeverk", "vare"));
        events.add(createEvent("utdanning", "vurdering", "fravarsoversikt"));

        def eventService = new EventService(Mock(EventConsumerFactoryService), events)

        when:
        def result = eventService.getEvents(ORG_ID, "", "", "", 0)

        then:
        result.size() == 5
    }

    def "Test getEvents with org filter"() {
        given:
        def events = new ArrayList<>();
        events.add(createEvent("test.no","utdanning", "vurdering", "fravar"));
        events.add(createEvent("vigoiks.no", "okonomi", "kodeverk", "vare"));
        events.add(createEvent("test.no", "utdanning", "vurdering", "fravar"));
        events.add(createEvent("test.no", "okonomi", "kodeverk", "vare"));
        events.add(createEvent("vigoiks.no", "utdanning", "vurdering", "fravarsoversikt"));

        def eventService = new EventService(Mock(EventConsumerFactoryService), events)

        when:
        def result = eventService.getEvents("vigoiks.no", "", "", "", 0)

        then:
        result.size() == 2
    }

    def "Test getEvents with domain-package-resource filter"() {
        given:
        def events = new ArrayList<>();
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("okonomi", "kodeverk", "vare"));
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("okonomi", "kodeverk", "vare"));
        events.add(createEvent("utdanning", "vurdering", "fravarsoversikt"));

        def eventService = new EventService(Mock(EventConsumerFactoryService), events)

        when:
        def result = eventService.getEvents(ORG_ID,"utdanning", "vurdering", "fravar", 0)

        then:
        result.size() == 2
    }

    def "Test getEvents with size limit"() {
        given:
        List<RequestFintEvent> events = new ArrayList<>();
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("utdanning", "vurdering", "fravar"));

        EventConsumerFactoryService kafkaFactoryService = Mock()
        EventService eventService = new EventService(kafkaFactoryService, events)

        when:
        def result = eventService.getEvents(ORG_ID,"utdanning", "vurdering", "fravar", 3)

        then:
        result.size() == 3
    }

    private RequestFintEvent createEvent(String domainName, String packageName, String resourceName) {
        return createEvent(ORG_ID, domainName, packageName, resourceName);
    }

    private RequestFintEvent createEvent(String orgId, String domainName, String packageName, String resourceName) {
        return RequestFintEvent.builder()
                .orgId(orgId)
                .domainName(domainName)
                .packageName(packageName)
                .resourceName(resourceName)
                .build();
    }
}
