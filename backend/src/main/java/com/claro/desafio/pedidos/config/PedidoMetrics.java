package com.claro.desafio.pedidos.config;

import com.claro.desafio.pedidos.domain.StatusPedido;
import com.claro.desafio.pedidos.repository.PedidoRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Todas as metricas aqui sao Gauges (nao Counters) porque representam um
 * estado atual consultado sob demanda a cada scrape (via PedidoRepository),
 * nao um total de eventos que so cresce - uma mudanca de status, exclusao
 * ou criacao deve refletir imediatamente na metrica, sem incrementar/
 * decrementar manualmente a cada operacao do PedidoService. (pedidos_total,
 * por ser cumulativo, e um Counter definido em PedidoService.) Alimentam o
 * dashboard de negocio no Grafana (cards de resumo: total, em
 * processamento, peso total, itens totais).
 */
@Component
@RequiredArgsConstructor
public class PedidoMetrics {

    private final PedidoRepository pedidoRepository;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void registrarMetricas() {
        for (StatusPedido status : StatusPedido.values()) {
            Gauge.builder("pedidos_by_status", pedidoRepository, repo -> repo.countByStatus(status))
                    .description("Total de pedidos por status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }

        Gauge.builder("pedidos_peso_total_gramas", pedidoRepository, PedidoRepository::somaPeso)
                .description("Soma do peso (em gramas) de todos os pedidos cadastrados")
                .register(meterRegistry);

        // nome sem sufixo "_total": o PrometheusNamingConvention remove esse
        // sufixo de Gauges (reservado para Counters), entao "pedidos_itens_total"
        // seria exposto como "pedidos_itens" mesmo assim - melhor ja nomear
        // do jeito que ele efetivamente fica exposto.
        Gauge.builder("pedidos_itens", pedidoRepository, PedidoRepository::somaItens)
                .description("Soma da quantidade de itens de todos os pedidos cadastrados")
                .register(meterRegistry);
    }
}
