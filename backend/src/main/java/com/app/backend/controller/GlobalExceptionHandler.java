package com.app.backend.controller;

import com.app.backend.exception.ApiException;
import com.app.backend.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail body = ex.updateAndGetBody(getMessageSource(), null);
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fieldMessage(fe));
        }
        body.setProperty("errors", errors);
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    /**
     * Type-mismatch binding failures (e.g. an unknown enum value in a query
     * parameter) carry Spring's verbose default message; replace it with the
     * rejected value so the client sees which input was invalid.
     */
    private static String fieldMessage(FieldError fe) {
        if (fe.contains(org.springframework.beans.TypeMismatchException.class)) {
            return "invalid value: " + fe.getRejectedValue();
        }
        return fe.getDefaultMessage();
    }

    @ExceptionHandler(AuthException.class)
    ResponseEntity<ProblemDetail> handleAuthException(AuthException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        body.setProperty("code", ex.getCode());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        String detail = ex.getMessage() != null ? ex.getMessage() : ex.getStatus().getReasonPhrase();
        return ResponseEntity.status(ex.getStatus())
                .body(ProblemDetail.forStatusAndDetail(ex.getStatus(), detail));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
