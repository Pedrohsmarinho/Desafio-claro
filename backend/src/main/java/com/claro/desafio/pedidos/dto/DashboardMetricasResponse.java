package com.claro.desafio.pedidos.dto;

import com.claro.desafio.pedidos.domain.StatusPedido;

import java.util.Map;

public record DashboardMetricasResponse(
        long totalPedidos,
        Map<StatusPedido, Long> porStatus,
        int limiteMaximo
) {
}
