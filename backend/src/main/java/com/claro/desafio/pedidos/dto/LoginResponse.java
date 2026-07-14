package com.claro.desafio.pedidos.dto;

/**
 * token permanece null ate o diferencial de autenticacao JWT ser implementado
 * (ver AuthService); o campo ja existe aqui para nao quebrar o contrato da
 * API quando o diferencial for adicionado.
 */
public record LoginResponse(
        String email,
        String token
) {
}
