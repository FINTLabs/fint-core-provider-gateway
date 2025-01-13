package no.fintlabs.provider.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class ExceptionController {

    @ExceptionHandler(InvalidResponseFintEventException.class)
    public ResponseEntity<String> handleInvalidResponseFintEventException(Throwable e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(AdapterNotRegisteredException.class)
    public ResponseEntity<String> handleAdapterNotRegisteredException(Throwable e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(InvalidSyncPageEntryException.class)
    public ResponseEntity<String> handleInvalidSyncPageEntryException(Throwable e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(InvalidAdapterCapabilityException.class)
    public ResponseEntity<String> handleInvalidAdapterCapabilityException(Throwable e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(MissingRoleException.class)
    public ResponseEntity<String> handleMissingRoleException(Throwable e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(CapabilityNotSupportedException.class)
    public ResponseEntity<String> handleCapabilityNotSupportedException(Throwable e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Void> handleJsonProcessingException(Throwable e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(InvalidOrgId.class)
    public ResponseEntity<String> handleInvalidOrgId(InvalidOrgId e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(InvalidUsername.class)
    public ResponseEntity<String> handleInvalidUsername(InvalidUsername e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedAdapterAccessException.class)
    public ResponseEntity<String> handleInvalidUsername(UnauthorizedAdapterAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler({NoRequestFoundException.class})
    public ResponseEntity<?> handleNoRequestFoundException(NoRequestFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({InvalidOrgIdException.class})
    public ResponseEntity<?> handleInvalidOrgIdException(InvalidOrgIdException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @ExceptionHandler({InvalidJwtException.class})
    public ResponseEntity<?> handleInvalidJwtException(InvalidJwtException exception) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(UnknownTopicOrPartitionException.class)
    public ResponseEntity<String> handleUnknownTopicOrPartitionException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("""
                The adapter has probably not called the '/register' endpoint. \
                Also, you need to check if the entity endpoint is in the capability list.\
                """);
    }

}
