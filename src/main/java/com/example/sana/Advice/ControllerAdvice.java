package com.example.sana.Advice;

import com.example.sana.API.ApiException;
import com.example.sana.API.ApiConflictException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.example.sana.API.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.ConstraintViolationException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;

@RestControllerAdvice
public class ControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ControllerAdvice.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse> handleStatusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ApiResponse(e.getReason()));
    }

    @ExceptionHandler(ApiConflictException.class)
    public ResponseEntity<ApiResponse> handleConflict(ApiConflictException e) {
        return ResponseEntity.status(409).body(new ApiResponse(e.getMessage()));
    }

    @ExceptionHandler({ConcurrencyFailureException.class, PessimisticLockException.class,
            LockTimeoutException.class, OptimisticLockException.class})
    public ResponseEntity<ApiResponse> handleConcurrentWrite(Exception e) {
        return ResponseEntity.status(409).body(new ApiResponse("Concurrent change detected. Reload the graph and retry"));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse> handleRequestParameter(Exception e) {
        return ResponseEntity.badRequest().body(new ApiResponse("A required request parameter is missing or invalid"));
    }

    // Api Exception
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse> handleApiException(
            ApiException e) {

        // Print real error in console
        // Do not log raw payload/provider exception content.

        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }

    // Handles invalid arguments supplied to application services.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(
            IllegalArgumentException e) {

        return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
    }



    // Validation Exception
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        return ResponseEntity.status(400).body(new ApiResponse(message));
    }


    // Constraint Violation Exception
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleConstraintViolationException(
            ConstraintViolationException e) {

        // Do not log raw payload/provider exception content.

        return ResponseEntity.status(400).body(new ApiResponse(e.getMessage()));
    }


    // Database Exception
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {

        // Do not log raw payload/provider exception content.

        return ResponseEntity.status(400)
                .body(new ApiResponse("Stored data conflicts with this operation"));
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleUnreadableRequest(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof InvalidFormatException invalidFormat
                    && invalidFormat.getTargetType() != null
                    && invalidFormat.getTargetType().isEnum()) {
                String message = "Invalid " + invalidFormat.getTargetType().getSimpleName()
                        + ". Allowed values: " + Arrays.toString(invalidFormat.getTargetType().getEnumConstants());
                return ResponseEntity.status(400).body(new ApiResponse(message));
            }
            cause = cause.getCause();
        }
        return ResponseEntity.status(400)
                .body(new ApiResponse("Request body contains malformed JSON or an invalid value"));
    }

    // General Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(
            Exception e) {

        // Log the full error on the server only, the client still gets a generic message.
        // n8n HTTP errors are handled in AIDispatchWorker and never reach this handler.
        log.error("Unexpected error while handling request", e);

        return ResponseEntity.status(500).body(new ApiResponse("The operation could not be completed"));
    }
}
