package no.fintlabs.provider

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@Suppress("DEPRECATION")
class TrailingSlashTest {

    private val mockMvc = MockMvcBuilders
        .standaloneSetup(
            ProviderController(
                requestValidator = mockk(),
                registrationService = mockk(),
                heartbeatService = mockk(),
                syncPageService = mockk(),
            )
        )
        .apply { setUseTrailingSlashPatternMatch(true) }
        .build()

    @Test
    fun `status without trailing slash matches a handler`() {
        check(handlerMatches("/status")) { "expected /status to match a handler" }
    }

    @Test
    fun `status with trailing slash also matches a handler`() {
        check(handlerMatches("/status/")) {
            "expected /status/ to match a handler with trailing-slash config enabled"
        }
    }

    // MockMvc throws ServletException when the handler is invoked but errors internally
    // (here: CorePrincipal resolves to null in standalone mode, causing NPE). That confirms
    // a handler matched. A clean 404 response means no handler matched.
    private fun handlerMatches(path: String): Boolean {
        return try {
            val statusCode = mockMvc.perform(get(path)).andReturn().response.status
            statusCode != 404
        } catch (_: Exception) {
            true
        }
    }
}
