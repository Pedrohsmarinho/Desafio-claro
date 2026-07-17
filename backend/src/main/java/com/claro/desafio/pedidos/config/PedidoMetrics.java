package com.claro.desafio.pedidos.config;

import com.claro.desafio.pedidos.domain.StatusPedido;
import com.claro.desafio.pedidos.repository.PedidoRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Gauges (nao Counters): refletem o estado atual a cada scrape, sem incrementar/decrementar manualmente. */
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

        // sem sufixo "_total": Prometheus remove esse sufixo de Gauges de qualquer forma
        Gauge.builder("pedidos_itens", pedidoRepository, PedidoRepository::somaItens)
                .description("Soma da quantidade de itens de todos os pedidos cadastrados")
                .register(meterRegistry);
    }
}
