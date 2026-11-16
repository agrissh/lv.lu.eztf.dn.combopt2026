package lv.lu.eztf.dn.combopt.matchmaking.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class JsonExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(JsonExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handle(
            HttpMessageNotReadableException ex) {

        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof JsonMappingException e) {
            log.error("JSON path: {}", e.getPathReference());
            log.error("JSON location: {}", e.getLocation());
            log.error("Deserialization failed", e);
        } else {
            log.error("Invalid JSON", ex);
        }

        return ResponseEntity.badRequest().body("Invalid JSON");
    }
}
