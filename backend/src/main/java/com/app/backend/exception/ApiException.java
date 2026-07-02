package com.app.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Framework-agnostic exception thrown by services to signal an HTTP outcome
 * without depending on Spring MVC's {@code ResponseStatusException}. Mapped to a
 * response by {@code GlobalExceptionHandler}.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status) {
        this(status, null);
    }

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
