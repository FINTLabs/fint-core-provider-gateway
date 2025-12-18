package no.fintlabs.provider.event.request

import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.models.event.ResponseFintEvent
import no.fintlabs.provider.event.response.ResponseEventTopicProducer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.Optional
import java.util.function.Consumer

@Service
class RequestEventService(
    private val requestCache: RequestCache,
    private val responseProducer: ResponseEventTopicProducer
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        requestCache.onExpired = Consumer { event -> sendExpiredResponse(event) }
    }

    fun getEvents(
        assets: Set<String>,
        domainName: String?,
        packageName: String?,
        resourceName: String?,
        size: Int
    ): List<RequestFintEvent> {
        val stream = requestCache.getAll()
            .filter { assets.contains(it.orgId) }
            .filter { domainName.isNullOrBlank() || it.domainName.equals(domainName, ignoreCase = true) }
            .filter { packageName.isNullOrBlank() || it.packageName.equals(packageName, ignoreCase = true) }
            .filter { resourceName.isNullOrBlank() || it.resourceName.equals(resourceName, ignoreCase = true) }

        return if (size > 0) stream.take(size).toList() else stream.toList()
    }

    fun addEvent(event: RequestFintEvent) {
        if (requestCache.add(event)) {
            logger.debug("Event with corrId: ${event.corrId} added")
        }
    }

    fun removeEvent(corrId: String) {
        requestCache.remove(corrId)
        logger.debug("Event with corrId: $corrId removed")
    }

    fun getEvent(corrId: String): Optional<RequestFintEvent> = Optional.ofNullable(requestCache.get(corrId))

    /**
     * Caffeine triggers this automatically when an item expires.
     */
    private fun sendExpiredResponse(request: RequestFintEvent) {
        logger.info("Event ${request.corrId} expired. Sending expired response.")
        responseProducer.sendEvent(request.toResponse(), request)
    }

    private fun RequestFintEvent.toResponse(): ResponseFintEvent =
        ResponseFintEvent().apply {
            corrId = this@toResponse.corrId
            orgId = this@toResponse.orgId
            handledAt = System.currentTimeMillis()
            isFailed = true
            errorMessage = "Event expired after waiting ${Duration.ofMillis(timeToLive).toMinutes()} minutes."
        }

}