package com.claro.desafio.pedidos.dto;

/** token e o JWT emitido em AuthService, usado no header Authorization: Bearer das demais requisicoes. */
public record LoginResponse(
        String email,
        String token
) {
}
