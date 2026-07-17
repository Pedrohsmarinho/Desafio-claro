package com.claro.desafio.pedidos.service;

import com.claro.desafio.pedidos.domain.Pedido;
import com.claro.desafio.pedidos.domain.StatusPedido;
import com.claro.desafio.pedidos.dto.DashboardMetricasResponse;
import com.claro.desafio.pedidos.dto.PedidoRequest;
import com.claro.desafio.pedidos.repository.PedidoRepository;
import com.claro.desafio.pedidos.repository.UsuarioRepository;
import com.claro.desafio.pedidos.service.exception.LimiteExcedidoException;
import com.claro.desafio.pedidos.service.exception.PedidoNaoEncontradoException;
import com.claro.desafio.pedidos.service.exception.TransicaoInvalidaException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Counter pedidosTotalCounter;

    @Value("${app.pedidos.limite-maximo}")
    private int limiteMaximo;

    public PedidoService(PedidoRepository pedidoRepository, UsuarioRepository usuarioRepository, MeterRegistry meterRegistry) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidosTotalCounter = Counter.builder("pedidos_total")
                .description("Total de pedidos criados (cumulativo, nao decresce com exclusoes)")
                .register(meterRegistry);
    }

    public List<Pedido> listarTodos(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId);
    }

    public Page<Pedido> buscar(Long usuarioId, StatusPedido status, String busca, Pageable pageable) {
        String buscaNormalizada = (busca == null || busca.isBlank()) ? null : busca.trim();
        return pedidoRepository.buscar(usuarioId, status, buscaNormalizada, pageable);
    }

    public DashboardMetricasResponse buscarMetricasDashboard(Long usuarioId) {
        Map<StatusPedido, Long> porStatus = new EnumMap<>(StatusPedido.class);
        for (StatusPedido status : StatusPedido.values()) {
            porStatus.put(status, pedidoRepository.countByUsuarioIdAndStatus(usuarioId, status));
        }
        long total = pedidoRepository.countByUsuarioId(usuarioId);
        return new DashboardMetricasResponse(total, porStatus, limiteMaximo);
    }

    public Pedido buscarPorId(Long id, Long usuarioId) {
        return pedidoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }

    @Transactional
    public Pedido criar(PedidoRequest request, Long usuarioId) {
        usuarioRepository.buscarPorIdComLock(usuarioId);

        long totalAtual = pedidoRepository.countByUsuarioId(usuarioId);
        if (totalAtual >= limiteMaximo) {
            log.warn("Usuario id={} tentou criar pedido acima do limite maximo ({}). Total atual: {}",
                    usuarioId, limiteMaximo, totalAtual);
            throw new LimiteExcedidoException(
                    "Limite maximo de " + limiteMaximo + " pedidos cadastrados foi atingido");
        }

        Pedido pedido = new Pedido();
        pedido.setDisplayName(request.displayName());
        pedido.setItens(request.itens());
        pedido.setPeso(request.peso());
        pedido.setStatus(StatusPedido.EM_PROCESSAMENTO);
        pedido.setUsuarioId(usuarioId);

        Pedido salvo = pedidoRepository.save(pedido);
        pedidosTotalCounter.increment();
        log.info("Pedido criado: id={}, usuarioId={}, cliente='{}', itens={}, peso={}g, status={}",
                salvo.getId(), usuarioId, salvo.getDisplayName(), salvo.getItens(), salvo.getPeso(), salvo.getStatus());
        return salvo;
    }

    public Pedido alterarStatus(Long id, StatusPedido novoStatus, Long usuarioId) {
        Pedido pedido = buscarPorId(id, usuarioId);
        StatusPedido statusAtual = pedido.getStatus();

        if (!statusAtual.podeTransicionarPara(novoStatus)) {
            log.warn("Transicao de status invalida para pedido id={} (usuarioId={}): {} -> {}",
                    id, usuarioId, statusAtual, novoStatus);
            throw new TransicaoInvalidaException(
                    "Transicao invalida de " + statusAtual + " para " + novoStatus);
        }

        pedido.setStatus(novoStatus);
        Pedido atualizado = pedidoRepository.save(pedido);
        log.info("Status do pedido id={} (usuarioId={}) alterado: {} -> {}", id, usuarioId, statusAtual, novoStatus);
        return atualizado;
    }

    public void excluir(Long id, Long usuarioId) {
        Pedido pedido = buscarPorId(id, usuarioId);
        pedidoRepository.deleteById(pedido.getId());
        log.info("Pedido excluido: id={}, usuarioId={}, cliente='{}'", id, usuarioId, pedido.getDisplayName());
    }
}
