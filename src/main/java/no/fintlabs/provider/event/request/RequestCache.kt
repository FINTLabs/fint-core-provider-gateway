package no.fintlabs.provider.event.request

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.RemovalCause
import no.fintlabs.adapter.models.event.RequestFintEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.math.max

@Component
class RequestCache(
    private val clock: Clock = Clock.systemUTC()
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val defaultTtl = TimeUnit.MINUTES.toMillis(2)
    private val tombstoneTtlMins = 30L

    var onExpired: Consumer<RequestFintEvent>? = null

    private val requestCache: Cache<String, RequestFintEvent> = Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfter(RequestExpiryPolicy())
        .removalListener(::handleRemoval)
        .build()

    private val tombstoneCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(tombstoneTtlMins, TimeUnit.MINUTES)
        .maximumSize(100_000)
        .build()

    fun add(event: RequestFintEvent): Boolean {
        if (isTombstoned(event.corrId)) {
            logger.debug("Event ${event.corrId} ignored: Tombstone found.")
            return false
        }

        // Apply default if missing
        if (event.timeToLive <= 0) event.timeToLive = defaultTtl

        if (event.isExpired()) {
            logger.debug("Event ${event.corrId} rejected: Arrived expired.")
            onExpired?.accept(event)
            return false
        }

        requestCache.put(event.corrId, event)
        return true
    }

    fun remove(corrId: String) {
        tombstoneCache.put(corrId, true)
        requestCache.invalidate(corrId)
    }

    fun get(corrId: String): RequestFintEvent? = requestCache.getIfPresent(corrId)

    fun getAll(): Sequence<RequestFintEvent> = requestCache.asMap().values.asSequence()

    private fun isTombstoned(corrId: String): Boolean = tombstoneCache.getIfPresent(corrId) != null

    private fun RequestFintEvent.isExpired(): Boolean = (clock.millis() - created) >= timeToLive

    /**
     * Handles Caffeine eviction.
     * CRITICAL: Only trigger callback if cause is EXPIRED.
     * explicit removal (EXPLICIT) or replacement (REPLACED) should be ignored.
     */
    private fun handleRemoval(key: String?, event: RequestFintEvent?, cause: RemovalCause) {
        if (cause == RemovalCause.EXPIRED && event != null) {
            onExpired?.accept(event)
        }
    }

    /**
     * Calculates the exact nanoseconds remaining for a specific item
     * based on its creation timestamp.
     */
    private inner class RequestExpiryPolicy : Expiry<String, RequestFintEvent> {
        override fun expireAfterCreate(key: String, value: RequestFintEvent, now: Long): Long {
            val expirationTime = value.created + value.timeToLive
            val remainingMillis = expirationTime - clock.millis()

            // Caffeine requires non-negative nanoseconds
            return TimeUnit.MILLISECONDS.toNanos(max(0, remainingMillis))
        }

        // We don't extend life on update/read, so just return currentDuration
        override fun expireAfterUpdate(k: String, v: RequestFintEvent, t: Long, d: Long) = d
        override fun expireAfterRead(k: String, v: RequestFintEvent, t: Long, d: Long) = d
    }
}