package org.legend8883.competencytestingsystem.controller;

import org.legend8883.competencytestingsystem.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Единая обработка ошибок, чтобы фронт получал понятный message,
 * а не 500 Internal Server Error на обычных бизнес-ошибках.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        // Для бизнес-ошибок используем 400, чтобы фронт показывал ex.getMessage()
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage() != null ? ex.getMessage() : "Ошибка"));
    }
}
