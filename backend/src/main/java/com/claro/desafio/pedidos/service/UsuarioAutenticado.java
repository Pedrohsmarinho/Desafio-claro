package com.claro.desafio.pedidos.service;

import com.claro.desafio.pedidos.domain.Usuario;

/** Retorno interno de AuthService - entidade + token, nao um DTO de resposta. */
public record UsuarioAutenticado(Usuario usuario, String token) {
}
