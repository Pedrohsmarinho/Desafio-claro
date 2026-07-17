package com.claro.desafio.pedidos.dto;

import com.claro.desafio.pedidos.domain.StatusPedido;

import java.util.Map;

/** Escopado ao usuario logado - distinto das metricas globais do Prometheus/Grafana. */
public record DashboardMetricasResponse(
        long totalPedidos,
        Map<StatusPedido, Long> porStatus,
        int limiteMaximo
) {
}
