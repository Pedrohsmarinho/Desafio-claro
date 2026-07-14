package com.claro.desafio.pedidos.dto;

import com.claro.desafio.pedidos.domain.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record PedidoStatusUpdateRequest(

        @NotNull(message = "Novo status e obrigatorio")
        StatusPedido status
) {
}
