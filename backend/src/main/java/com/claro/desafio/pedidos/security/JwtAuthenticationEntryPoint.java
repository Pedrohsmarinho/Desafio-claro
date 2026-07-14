package com.claro.desafio.pedidos.security;

import com.claro.desafio.pedidos.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Retorna 401 em JSON (mesmo formato do GlobalExceptionHandler) quando uma
 * rota protegida e acessada sem token, com token invalido/expirado, ou com
 * um token cujo usuario nao existe mais.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /** ObjectMapper autoconfigurado pelo Spring Boot, com o modulo JSR-310 (java.time) ja registrado. */
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Token ausente, invalido ou expirado",
                request.getRequestURI());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
