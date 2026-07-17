package com.claro.desafio.pedidos.dto;

import com.claro.desafio.pedidos.domain.StatusPedido;

public record PedidoResponse(
        Long id,
        String displayName,
        Integer itens,
        Long peso,
        double pesoKg,
        StatusPedido status
) {
}
