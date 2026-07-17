package com.claro.desafio.pedidos.service;

import com.claro.desafio.pedidos.domain.Usuario;

public record UsuarioAutenticado(Usuario usuario, String token) {
}
