package no.fintlabs.provider.event.request

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
import kotlin.time.Duration.Companion.minutes

class RequestCacheTest {

    private lateinit var clock: Clock
    private lateinit var requestCache: RequestCache
    private lateinit var onExpiredMock: Consumer<RequestFintEvent>

    private val corrId = "id-1"

    @BeforeEach
    fun setup() {
        clock = Clock.fixed(Instant.parse("2023-01-01T12:00:00.00Z"), ZoneId.of("UTC"))
        onExpiredMock = mockk(relaxed = true)

        requestCache = RequestCache(clock)
        requestCache.onExpired = onExpiredMock
    }

    @Test
    fun `should add event to cache if valid`() {
        val event = createEvent(corrId, 10000L)

        val result = requestCache.add(event)

        assertThat(result).isTrue
        assertThat(requestCache.get(corrId)).isEqualTo(event)
    }

    @Test
    fun `should reject event if it arrives already expired`() {
        // Created 20 seconds ago, TTL is 10 seconds
        val createdOffset = clock.millis() - 20000L
        val event = createEvent(corrId, 10000L, created = createdOffset)

        val result = requestCache.add(event)

        assertThat(result).isFalse

        // Should trigger the expired callback immediately
        verify(exactly = 1) { onExpiredMock.accept(event) }
    }

    @Test
    fun `should set default TTL if Null`() {
        val event = createEvent(corrId) // TTL 0

        requestCache.add(event)

        // Default TTL in code is 2 minutes (120000ms)
        assertThat(event.timeToLive).isEqualTo(event.created + 120000L)
    }

    @Test
    fun `should set default TTL if not provided`() {
        // If created and ttl is equal then it is the same as if TTL was not provided.
        val event = createEvent(corrId, 0L, 0L)

        requestCache.add(event)

        // Default TTL in code is 2 minutes (120000ms)
        assertThat(event.timeToLive).isEqualTo(event.created + 120000L)
    }

    @Test
    fun `remove should invalidate cache and create tombstone`() {
        val event = createEvent(corrId, 10000L)
        requestCache.add(event)

        requestCache.remove(corrId)

        assertThat(requestCache.get(corrId)).isNull()

        // Try to add it again - should fail due to tombstone
        val addedAgain = requestCache.add(event)
        assertThat(addedAgain).isFalse()
    }

    @Test
    fun `remove (explicit) should NOT trigger onExpired callback`() {
        val event = createEvent(corrId, 100000L)
        requestCache.add(event)

        // Explicit removal
        requestCache.remove(corrId)

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

    // --- Helpers ---

    private fun createEvent(
        corrId: String,
        ttl: Long = 2.minutes.inWholeMilliseconds,
        created: Long = clock.millis()
    ): RequestFintEvent =
        RequestFintEvent().apply {
            this.corrId = corrId
            orgId = "fint.no"
            this.created = created
            timeToLive = created + ttl
        }



}