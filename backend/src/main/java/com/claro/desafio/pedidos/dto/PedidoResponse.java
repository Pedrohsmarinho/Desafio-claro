package com.claro.desafio.pedidos.dto;

import com.claro.desafio.pedidos.domain.StatusPedido;

/**
 * pesoKg e derivado de peso (gramas) apenas para conveniencia do frontend;
 * a fonte da verdade continua sendo o peso em gramas. A conversao
 * Pedido -> PedidoResponse e feita pelo PedidoMapper (MapStruct), nunca
 * manualmente.
 */
public record PedidoResponse(
        Long id,
        String displayName,
        Integer itens,
        Long peso,
        double pesoKg,
        StatusPedido status
) {
}
