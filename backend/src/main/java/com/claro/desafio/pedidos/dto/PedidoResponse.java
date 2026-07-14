package com.claro.desafio.pedidos.dto;

import com.claro.desafio.pedidos.domain.Pedido;
import com.claro.desafio.pedidos.domain.StatusPedido;

/**
 * pesoKg e derivado de peso (gramas) apenas para conveniencia do frontend;
 * a fonte da verdade continua sendo o peso em gramas.
 */
public record PedidoResponse(
        Long id,
        String displayName,
        Integer itens,
        Long peso,
        double pesoKg,
        StatusPedido status
) {
    public static PedidoResponse from(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getDisplayName(),
                pedido.getItens(),
                pedido.getPeso(),
                pedido.getPeso() / 1000.0,
                pedido.getStatus()
        );
    }
}
