package no.fintlabs.event;

import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.event.request.RequestEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class RequestEventServiceTest {

    private final String ORG_ID = "vigoiks.no";

    private RequestEventService eventService;

    @BeforeEach
    public void setup() {
        eventService = new RequestEventService();
    }

    @Test
    public void testGetEvents() {
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("okonomi", "kodeverk", "vare"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravar"));
        eventService.addEvent(createEvent("okonomi", "kodeverk", "vare"));
        eventService.addEvent(createEvent("utdanning", "vurdering", "fravarsoversikt"));

        List<RequestFintEvent> result = eventService.getEvents(ORG_ID, "", "", "", 0);

        assertEquals(5, result.size());
    }

    @Test
    public void testAddEvent() {
        RequestFintEvent event = createEvent("utdanning", "vurdering", "fravar");
        eventService.addEvent(event);
        List<RequestFintEvent> result = eventService.getEvents(ORG_ID, "", "", "", 0);
        assertEquals(1, result.size());
        assertEquals(event, result.get(0));
    }

    @Test
    public void testRemoveEvent() {
        RequestFintEvent event = createEvent("utdanning", "vurdering", "fravar");
        eventService.addEvent(event);
        eventService.removeEvent(event.getCorrId());
        List<RequestFintEvent> result = eventService.getEvents(ORG_ID, "", "", "", 0);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetEvent() {
        RequestFintEvent event = createEvent("utdanning", "vurdering", "fravar");
        eventService.addEvent(event);
        Optional<RequestFintEvent> result = eventService.getEvent(event.getCorrId());
        assertTrue(result.isPresent());
        assertEquals(event, result.get());
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