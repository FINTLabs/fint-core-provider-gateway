package no.fintlabs.provider.event.request

import io.mockk.*
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.provider.event.response.ResponseEventTopicProducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.Consumer

class RequestEventServiceTest {

    private lateinit var requestCache: RequestCache
    private lateinit var responseProducer: ResponseEventTopicProducer
    private lateinit var service: RequestEventService

    @BeforeEach
    fun setup() {
        requestCache = mockk(relaxed = true)
        responseProducer = mockk(relaxed = true)

        val consumerSlot = slot<Consumer<RequestFintEvent>>()
        every { requestCache.onExpired = capture(consumerSlot) } just Runs

        service = RequestEventService(requestCache, responseProducer)
    }

    @Test
    fun `init should register expiration callback`() {
        verify { requestCache.onExpired = any() }
    }

    @Test
    fun `callback should send expired response when event expires`() {
        val callbackSlot = slot<Consumer<RequestFintEvent>>()
        verify { requestCache.onExpired = capture(callbackSlot) }
        val registeredCallback = callbackSlot.captured

        val requestEvent = RequestFintEvent().apply {
            corrId = "123"
            orgId = "fint.no"
            timeToLive = 60000
        }

        // Manually invoke the callback (simulating cache calling it)
        registeredCallback.accept(requestEvent)

        // Verify Producer was called with correct response
        val responseSlot = slot<ResponseFintEvent>()
        verify { responseProducer.sendEvent(capture(responseSlot), requestEvent) }

        val capturedResponse = responseSlot.captured
        assertThat(capturedResponse.corrId).isEqualTo("123")
        assertThat(capturedResponse.isFailed).isTrue
        assertThat(capturedResponse.errorMessage).contains("Event expired")
    }

    @Test
    fun `getEvents should filter correctly`() {
        val event1 = createEvent("1", "orgA", "domainA", "pkgA", "resA")
        val event2 = createEvent("2", "orgA", "domainB", "pkgB", "resB")
        val event3 = createEvent("3", "orgB", "domainA", "pkgA", "resA") // Wrong Org

        every { requestCache.getAll() } returns sequenceOf(event1, event2, event3)

        // Filter by Org Only (event1, event2 match)
        val result1 = service.getEvents(setOf("orgA"), null, null, null, 0)
        assertThat(result1).containsExactlyInAnyOrder(event1, event2)

        // Filter by specific Domain (event1 matches)
        val result2 = service.getEvents(setOf("orgA"), "domainA", null, null, 0)
        assertThat(result2).containsExactly(event1)

        // Filter by package case insensitive
        val result3 = service.getEvents(setOf("orgA"), null, "PKGB", null, 0)
        assertThat(result3).containsExactly(event2)

        // Size limit
        val resultLimit = service.getEvents(setOf("orgA"), null, null, null, 1)
        assertThat(resultLimit).hasSize(1)
    }

    @Test
    fun `addEvent should delegate to cache`() {
        val event = RequestFintEvent().apply { corrId = "abc" }
        every { requestCache.add(event) } returns true

        service.addEvent(event)

        verify { requestCache.add(event) }
    }

    @Test
    fun `removeEvent should delegate to cache`() {
        service.removeEvent("abc")
        verify { requestCache.remove("abc") }
    }

    @Test
    fun `getEvent should delegate to cache`() {
        val event = RequestFintEvent()
        every { requestCache.get("abc") } returns event

        val result = service.getEvent("abc")

        assertThat(result).isPresent
        assertThat(result.get()).isEqualTo(event)
    }

    private fun createEvent(
        id: String,
        org: String,
        domain: String,
        pkg: String,
        res: String
    ): RequestFintEvent {
        return RequestFintEvent().apply {
            corrId = id
            orgId = org
            domainName = domain
            packageName = pkg
            resourceName = res
        }
    }
}