package no.fintlabs.event

import no.fintlabs.adapter.models.RequestFintEvent
import no.fintlabs.kafka.event.EventConsumerFactoryService
import spock.lang.Specification

class EventServiceSpec extends Specification {

    def "Test getEvents without filter"() {
        given:
        def events = new ArrayList<>();
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("okonomi", "kodeverk", "vare"));
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("okonomi", "kodeverk", "vare"));
        events.add(createEvent("utdanning", "vurdering", "fravarsoversikt"));

        def eventService = new EventService(Mock(EventConsumerFactoryService), events)

        when:
        def result = eventService.getEvents("", "", "", 0)

        then:
        result.size() == 5
    }

    def "Test getEvents with filter"() {
        given:
        def events = new ArrayList<>();
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("okonomi", "kodeverk", "vare"));
        events.add(createEvent("utdanning", "vurdering", "fravar"));
        events.add(createEvent("okonomi", "kodeverk", "vare"));
        events.add(createEvent("utdanning", "vurdering", "fravarsoversikt"));

        def eventService = new EventService(Mock(EventConsumerFactoryService), events)

        when:
        def result = eventService.getEvents("utdanning", "vurdering", "fravar", 0)

        then:
        result.size() == 2
    }

    def "Test getEvents with size"() {
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
        def result = eventService.getEvents("utdanning", "vurdering", "fravar", 3)

        then:
        result.size() == 3
    }

    private RequestFintEvent createEvent(String domainName, String packageName, String resourceName) {
        return RequestFintEvent.builder()
                .orgId("fintlabs.no")
                .domainName(domainName)
                .packageName(packageName)
                .resourceName(resourceName)
                .build();
    }
}
