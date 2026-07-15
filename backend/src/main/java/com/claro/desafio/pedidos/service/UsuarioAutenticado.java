package com.claro.desafio.pedidos.service;

import com.claro.desafio.pedidos.domain.Usuario;

/**
 * Retorno interno de AuthService (nao e um DTO de request/response) - carrega
 * a entidade Usuario e o token recem-gerado para o Controller montar o
 * LoginResponse via UsuarioMapper. O Service nunca conhece DTO de
 * request/response, so a entidade e o que precisa alem dela (o token).
 */
public record UsuarioAutenticado(Usuario usuario, String token) {
}
