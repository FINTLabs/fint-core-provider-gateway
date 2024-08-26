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

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Void> handleJsonProcessingException(Throwable e) {
        log.error(e.getMessage());
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

    @ExceptionHandler(UnknownTopicOrPartitionException.class)
    public ResponseEntity<String> handleUnknownTopicOrPartitionException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("""
                The adapter has probably not called the '/register' endpoint. \
                Also, you need to check if the entity endpoint is in the capability list.\
                """);
    }

}
