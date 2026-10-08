package com.oficinaagua.chatbot.exception;

import com.oficinaagua.chatbot.api.model.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Errores de validación del cuerpo (Bean Validation): 400 con el detalle por campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.status(status)
                .body(buildBody(status, "La solicitud no es valida.", details));
    }

    /** Resto de excepciones estándar de Spring MVC (JSON malformado, 405, 415...): mismo formato. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        String message = statusCode.is4xxClientError()
                ? "La solicitud no pudo procesarse."
                : "Ocurrio un error interno.";
        return ResponseEntity.status(statusCode).headers(headers)
                .body(buildBody(statusCode, message, List.of()));
    }

    /** LLM o base vectorial caídos: 503 con mensaje genérico; la causa queda solo en el log. */
    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<Object> handleAiUnavailable(AiServiceUnavailableException ex) {
        log.error("Servicio de IA no disponible", ex);
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status)
                .body(buildBody(status,
                        "El servicio no esta disponible temporalmente. Intenta de nuevo mas tarde.",
                        List.of()));
    }

    /** Cualquier error no previsto: 500 genérico. El detalle queda solo en el log del servidor. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Error inesperado", ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(buildBody(status, "Ocurrio un error interno.", List.of()));
    }

    private ErrorResponseDto buildBody(HttpStatusCode status, String message, List<String> details) {
        ErrorResponseDto body = new ErrorResponseDto();
        body.setTimestamp(OffsetDateTime.now());
        body.setStatus(status.value());
        body.setError(HttpStatus.valueOf(status.value()).getReasonPhrase());
        body.setMessage(message);
        body.setDetails(details);
        return body;
    }
}