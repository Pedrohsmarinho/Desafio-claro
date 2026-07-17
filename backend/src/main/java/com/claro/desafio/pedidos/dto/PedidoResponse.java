package com.claro.desafio.pedidos.dto;

import com.claro.desafio.pedidos.domain.StatusPedido;

/** pesoKg e so conveniencia do frontend; a fonte da verdade e peso (gramas). */
public record PedidoResponse(
        Long id,
        String displayName,
        Integer itens,
        Long peso,
        double pesoKg,
        StatusPedido status
) {
}
