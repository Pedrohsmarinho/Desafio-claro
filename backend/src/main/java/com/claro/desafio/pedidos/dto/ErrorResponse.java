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

    /**
     * Deriva "status" (numero) e "error" (frase padrao, ex: "Not Found") a
     * partir do proprio HttpStatus - usado por GlobalExceptionHandler e
     * JwtAuthenticationEntryPoint, os dois pontos que montam um corpo de erro
     * (o segundo roda no filtro de seguranca, antes do dispatch do Spring MVC,
     * por isso nao passa pelo primeiro).
     */
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status.value(), status.getReasonPhrase(), message, path);
    }
}
