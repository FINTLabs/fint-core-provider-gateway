package no.fintlabs.provider.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.provider.kafka.ProviderError;
import no.fintlabs.provider.kafka.ProviderErrorPublisher;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class ExceptionController {

    private final ProviderErrorPublisher providerErrorPublisher;

    @ExceptionHandler(InvalidResponseFintEventException.class)
    public ResponseEntity<String> handleInvalidResponseFintEventException(Throwable e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(AdapterNotRegisteredException.class)
    public ResponseEntity<String> handleAdapterNotRegisteredException(Throwable e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(InvalidSyncPageEntryException.class)
    public ResponseEntity<String> handleInvalidSyncPageEntryException(Throwable e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(InvalidAdapterCapabilityException.class)
    public ResponseEntity<String> handleInvalidAdapterCapabilityException(Throwable e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(MissingRoleException.class)
    public ResponseEntity<String> handleMissingRoleException(Throwable e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(CapabilityNotSupportedException.class)
    public ResponseEntity<String> handleCapabilityNotSupportedException(Throwable e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Void> handleJsonProcessingException(Throwable e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(InvalidOrgId.class)
    public ResponseEntity<String> handleInvalidOrgId(InvalidOrgId e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(InvalidUsername.class)
    public ResponseEntity<String> handleInvalidUsername(InvalidUsername e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedAdapterAccessException.class)
    public ResponseEntity<String> handleInvalidUsername(UnauthorizedAdapterAccessException e) {
        providerErrorPublisher.publish(ProviderError.from(e));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler({NoRequestFoundException.class})
    public ResponseEntity<?> handleNoRequestFoundException(NoRequestFoundException exception) {
        providerErrorPublisher.publish(ProviderError.from(exception));
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({InvalidOrgIdException.class})
    public ResponseEntity<?> handleInvalidOrgIdException(InvalidOrgIdException exception) {
        providerErrorPublisher.publish(ProviderError.from(exception));
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @ExceptionHandler({InvalidJwtException.class})
    public ResponseEntity<?> handleInvalidJwtException(InvalidJwtException exception) {
        providerErrorPublisher.publish(ProviderError.from(exception));
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(UnknownTopicOrPartitionException.class)
    public ResponseEntity<String> handleUnknownTopicOrPartitionException(UnknownTopicOrPartitionException exception) {
        providerErrorPublisher.publish(ProviderError.from(exception));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("""
                The adapter has probably not called the '/register' endpoint. \
                Also, you need to check if the entity endpoint is in the capability list.\
                """);
    }

}
