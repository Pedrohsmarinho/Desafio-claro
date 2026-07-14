package com.claro.desafio.pedidos.service;

import com.claro.desafio.pedidos.domain.Pedido;
import com.claro.desafio.pedidos.domain.StatusPedido;
import com.claro.desafio.pedidos.dto.PedidoRequest;
import com.claro.desafio.pedidos.repository.PedidoRepository;
import com.claro.desafio.pedidos.service.exception.LimiteExcedidoException;
import com.claro.desafio.pedidos.service.exception.PedidoNaoEncontradoException;
import com.claro.desafio.pedidos.service.exception.TransicaoInvalidaException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Todas as operacoes sao escopadas por usuarioId (multi-tenant): o limite de
 * 5 pedidos e por usuario (nao global), e buscar/alterar/excluir um pedido
 * que existe mas pertence a outro usuario resulta em
 * PedidoNaoEncontradoException (404) - nunca 403, para nao revelar que o
 * recurso existe e de quem e. O usuarioId sempre vem do JWT (via
 * PedidoController), nunca de parametro da requisicao.
 */
@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;

    @Value("${app.pedidos.limite-maximo}")
    private int limiteMaximo;

    public List<Pedido> listarTodos(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId);
    }

    public Pedido buscarPorId(Long id, Long usuarioId) {
        return pedidoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }

    public Pedido criar(PedidoRequest request, Long usuarioId) {
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
