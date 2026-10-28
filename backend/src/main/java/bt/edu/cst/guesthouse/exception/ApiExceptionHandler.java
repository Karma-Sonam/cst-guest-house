package bt.edu.cst.guesthouse.exception;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
    private ResponseEntity<Object> error(HttpStatusCode status,String message, Map<String,String> fields) {
        return ResponseEntity.status(status).body(Map.of("code",status.value(),"message",message,
            "fieldErrors",fields,"requestId",UUID.randomUUID().toString()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Object> status(ResponseStatusException e) {
        return error(e.getStatusCode(),Objects.requireNonNullElse(e.getReason(),"Request failed"),Map.of());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Object> validation(MethodArgumentNotValidException e) {
        Map<String,String> fields=new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(),f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST,"Validation failed",fields);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> conflict(DataIntegrityViolationException e) {
        return error(HttpStatus.CONFLICT,"The request conflicts with an existing record",Map.of());
    }
}
