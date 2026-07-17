package com.claro.desafio.pedidos.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }

    // deriva status/error do HttpStatus; usado por GlobalExceptionHandler e JwtAuthenticationEntryPoint
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status.value(), status.getReasonPhrase(), message, path);
    }
}
