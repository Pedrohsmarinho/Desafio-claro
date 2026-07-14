package com.claro.desafio.pedidos.config;

import com.claro.desafio.pedidos.domain.StatusPedido;
import com.claro.desafio.pedidos.repository.PedidoRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * pedidos_by_status e um Gauge (nao Counter) porque representa um estado
 * atual consultado sob demanda a cada scrape (via PedidoRepository), nao um
 * total de eventos que so cresce - uma mudanca de status ou exclusao deve
 * refletir imediatamente na metrica, sem incrementar/decrementar manualmente
 * a cada operacao do PedidoService. (pedidos_total, por ser cumulativo, e um
 * Counter definido em PedidoService.)
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
    }
}
