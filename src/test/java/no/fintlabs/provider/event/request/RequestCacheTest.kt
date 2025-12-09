package no.fintlabs.provider.event.request

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.fintlabs.adapter.models.event.RequestFintEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.function.Consumer

class RequestCacheTest {

    private lateinit var clock: Clock
    private lateinit var requestCache: RequestCache
    private lateinit var onExpiredMock: Consumer<RequestFintEvent>

    @BeforeEach
    fun setup() {
        // Fix time to: 2023-01-01T12:00:00Z
        clock = Clock.fixed(Instant.parse("2023-01-01T12:00:00.00Z"), ZoneId.of("UTC"))
        onExpiredMock = mockk(relaxed = true)

        requestCache = RequestCache(clock)
        requestCache.onExpired = onExpiredMock
    }

    @Test
    fun `should add event to cache if valid`() {
        val event = createEvent("id-1", 10000L)

        val result = requestCache.add(event)

        assertThat(result).isTrue
        assertThat(requestCache.get("id-1")).isEqualTo(event)
    }

    @Test
    fun `should reject event if it arrives already expired`() {
        // Created 20 seconds ago, TTL is 10 seconds
        val event = createEvent("id-expired", 10000L, createdOffset = -20000L)

        val result = requestCache.add(event)

        assertThat(result).isFalse
        assertThat(requestCache.get("id-expired")).isNull()

        // Should trigger the expired callback immediately
        verify(exactly = 1) { onExpiredMock.accept(event) }
    }

    @Test
    fun `should set default TTL if not provided`() {
        val event = createEvent("id-no-ttl", 0L) // TTL 0

        requestCache.add(event)

        // Default TTL in code is 2 minutes (120000ms)
        assertThat(event.timeToLive).isEqualTo(120000L)
    }

    @Test
    fun `remove should invalidate cache and create tombstone`() {
        val event = createEvent("id-remove", 10000L)
        requestCache.add(event)

        requestCache.remove("id-remove")

        assertThat(requestCache.get("id-remove")).isNull()

        // Try to add it again - should fail due to tombstone
        val addedAgain = requestCache.add(event)
        assertThat(addedAgain).isFalse()
    }

    @Test
    fun `remove (explicit) should NOT trigger onExpired callback`() {
        val event = createEvent("id-explicit-remove", 100000L)
        requestCache.add(event)

        // Explicit removal
        requestCache.remove("id-explicit-remove")

        verify(exactly = 0) { onExpiredMock.accept(any()) }
    }

    @Test
    fun `getAll should return all values in cache`() {
        requestCache.add(createEvent("1", 5000))
        requestCache.add(createEvent("2", 5000))

        val all = requestCache.getAll().toList()

        assertThat(all).hasSize(2)
        assertThat(all.map { it.corrId }).containsExactlyInAnyOrder("1", "2")
    }

    @Test
    fun `should keep provided TTL if it is greater than zero`() {
        val userProvidedTtl = 99999L
        val event = createEvent("id-custom-ttl", userProvidedTtl)

        requestCache.add(event)

        // Ensure it wasn't changed to the default (120000)
        assertThat(event.timeToLive).isEqualTo(userProvidedTtl)
    }

    // --- Helpers ---

    private fun createEvent(
        corrId: String,
        ttl: Long,
        createdOffset: Long = 0
    ): RequestFintEvent {
        val event = mockk<RequestFintEvent>(relaxed = true)
        every { event.corrId } returns corrId
        every { event.timeToLive } returns ttl
        every { event.timeToLive = any() } answers { every { event.timeToLive } returns firstArg() }

        val createdTime = clock.millis() + createdOffset
        every { event.created } returns createdTime

        return event
    }
}