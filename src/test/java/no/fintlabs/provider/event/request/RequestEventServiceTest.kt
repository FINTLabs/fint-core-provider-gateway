package no.fintlabs.provider.event.request

import io.mockk.*
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.provider.event.response.ResponseEventTopicProducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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

    @Nested
    inner class GetEvents {

        private val orgId = "fint.no"
        private val otherOrgId = "other.no"

        private val eventFullMatch = createEvent("1", orgId, "education", "student", "person")
        private val eventDiffRes = createEvent("2", orgId, "education", "student", "grades")
        private val eventDiffPkg = createEvent("3", orgId, "education", "teacher", "person")
        private val eventDiffDomain = createEvent("4", orgId, "administration", "staff", "person")
        private val eventUpperCase = createEvent("5", orgId, "EDUCATION", "STUDENT", "PERSON") // For case insensitive check
        private val eventOtherOrg = createEvent("7", otherOrgId, "education", "student", "person") // Should be filtered out by asset

        @BeforeEach
        fun setupCache() {
            // Mock the cache to return our defined list as a sequence
            every { requestCache.getAll() } returns sequenceOf(
                eventFullMatch,
                eventDiffRes,
                eventDiffPkg,
                eventDiffDomain,
                eventUpperCase,
                eventOtherOrg
            )
        }

        @Test
        fun `should return all events for a specific orgId (asset) ignoring others`() {
            val result = service.getEvents(assets = setOf(orgId))

            assertThat(result)
                .hasSize(5)
                .contains(eventFullMatch, eventDiffRes, eventDiffPkg, eventDiffDomain, eventUpperCase)
                .doesNotContain(eventOtherOrg)
        }

        @Test
        fun `should return empty list if orgId (asset) does not match any events`() {
            val result = service.getEvents(assets = setOf("non.existing.org"))

            assertThat(result).isEmpty()
        }

        @Test
        fun `should filter by domain name (case insensitive)`() {
            val result = service.getEvents(
                assets = setOf(orgId),
                domainName = "Education"
            )

            assertThat(result)
                .hasSize(4)
                .contains(eventFullMatch, eventDiffRes, eventDiffPkg, eventUpperCase)
                .doesNotContain(eventDiffDomain, )
        }

        @Test
        fun `should filter by domain and package name`() {
            val result = service.getEvents(
                assets = setOf(orgId),
                domainName = "education",
                packageName = "student"
            )

            assertThat(result)
                .hasSize(3)
                .contains(eventFullMatch, eventDiffRes, eventUpperCase)
                .doesNotContain(eventDiffPkg)
        }

        @Test
        fun `should filter by domain, package and resource name`() {
            val result = service.getEvents(
                assets = setOf(orgId),
                domainName = "education",
                packageName = "student",
                resourceName = "person"
            )

            assertThat(result)
                .hasSize(2)
                .contains(eventFullMatch, eventUpperCase)
                .doesNotContain(eventDiffRes)
        }

        @Test
        fun `should return empty list if filter criteria does not match`() {
            val result = service.getEvents(
                assets = setOf(orgId),
                domainName = "education",
                packageName = "non-existent-package"
            )

            assertThat(result).isEmpty()
        }

        @Test
        fun `should limit the result size when size parameter is greater than 0`() {
            // We have 6 valid events for this orgId
            val result = service.getEvents(
                assets = setOf(orgId),
                size = 2
            )

            assertThat(result).hasSize(2)
        }

        @Test
        fun `should return all matches when size parameter is 0`() {
            val result = service.getEvents(
                assets = setOf(orgId),
                size = 0
            )

            assertThat(result).hasSize(5)
        }

        @Test
        fun `should handle multiple assets in request`() {
            val result = service.getEvents(
                assets = setOf(orgId, otherOrgId)
            )

            assertThat(result)
                .hasSize(6)
                .contains(eventFullMatch, eventOtherOrg)
        }
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
        id: String?,
        org: String?,
        domain: String?,
        pkg: String?,
        res: String?
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