package no.fintlabs.provider.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.novari.resource.server.authentication.CorePrincipal
import no.novari.resource.server.enums.FintScope
import no.novari.resource.server.enums.FintType
import no.fintlabs.provider.kafka.ProviderError
import no.fintlabs.provider.kafka.ProviderErrorPublisher
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.net.URI

@Component
class SecurityProblemDetailHandler(
    private val objectMapper: ObjectMapper,
    private val providerErrorPublisher: ProviderErrorPublisher,
) : AccessDeniedHandler, AuthenticationEntryPoint {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        val detail = describeDenial()
        logger.warn("Access denied on {} {}: {}", request.method, request.requestURI, detail)
        providerErrorPublisher.publish(ProviderError.from(accessDeniedException))
        writeProblemDetail(
            request = request,
            response = response,
            status = HttpStatus.FORBIDDEN,
            title = "Forbidden",
            detail = detail,
        )
    }

    private fun describeDenial(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return when {
            auth !is CorePrincipal -> "Principal is not a FINT adapter"
            auth.type != FintType.ADAPTER -> "Principal type must be ADAPTER"
            FintScope.FINT_ADAPTER !in auth.scopes -> "JWT is missing required 'fint-adapter' scope"
            else -> "Adapter is missing required role for the requested component"
        }
    }

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        logger.warn("Authentication failed on {} {}: {}", request.method, request.requestURI, authException.message)
        providerErrorPublisher.publish(ProviderError.from(authException))
        writeProblemDetail(
            request = request,
            response = response,
            status = HttpStatus.UNAUTHORIZED,
            title = "Unauthorized",
            detail = authException.message ?: "Authentication is required",
        )
    }

    private fun writeProblemDetail(
        request: HttpServletRequest,
        response: HttpServletResponse,
        status: HttpStatus,
        title: String,
        detail: String,
    ) {
        val problem = ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.title = title
            this.instance = URI.create(request.requestURI)
        }
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.outputStream, problem)
    }
}
