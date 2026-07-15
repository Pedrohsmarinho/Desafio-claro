package com.claro.desafio.pedidos.controller;

import com.claro.desafio.pedidos.domain.Usuario;
import com.claro.desafio.pedidos.dto.DashboardMetricasResponse;
import com.claro.desafio.pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Metricas para o dashboard do usuario autenticado (cards/graficos do
 * frontend) - distinto de /actuator/prometheus: aquele expoe metricas
 * globais (todos os usuarios) para o Grafana, este responde "meus pedidos
 * agora", escopado ao dono do token. Ver PedidoService.buscarMetricasDashboard
 * e o README para a justificativa completa da distincao.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Metricas do usuario autenticado para os cards/graficos do frontend")
public class DashboardController {

    private final PedidoService pedidoService;

    @GetMapping("/metricas")
    @Operation(summary = "Total de pedidos, contagem por status e limite maximo do usuario autenticado")
    public DashboardMetricasResponse metricas(@AuthenticationPrincipal Usuario usuario) {
        return pedidoService.buscarMetricasDashboard(usuario.getId());
    }
}
