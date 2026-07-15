package com.claro.desafio.pedidos.dto;

import com.claro.desafio.pedidos.domain.StatusPedido;

import java.util.Map;

/**
 * Metricas do dashboard do usuario autenticado (cards e graficos do
 * frontend) - deliberadamente distinta das metricas de observabilidade
 * expostas em /actuator/prometheus (pedidos_total, pedidos_by_status):
 * aquelas sao globais (somam todos os usuarios) e alimentam o Grafana,
 * que responde "qual a saude/uso agregado do sistema"; esta aqui e
 * escopada ao usuario logado e responde "quantos pedidos EU tenho agora,
 * por status" - perguntas diferentes por design, ver README.
 */
public record DashboardMetricasResponse(
        long totalPedidos,
        Map<StatusPedido, Long> porStatus,
        int limiteMaximo
) {
}
