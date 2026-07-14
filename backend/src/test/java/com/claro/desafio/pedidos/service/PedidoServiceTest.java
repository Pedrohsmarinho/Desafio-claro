package com.claro.desafio.pedidos.service;

import com.claro.desafio.pedidos.domain.Pedido;
import com.claro.desafio.pedidos.domain.StatusPedido;
import com.claro.desafio.pedidos.dto.PedidoRequest;
import com.claro.desafio.pedidos.repository.PedidoRepository;
import com.claro.desafio.pedidos.service.exception.LimiteExcedidoException;
import com.claro.desafio.pedidos.service.exception.PedidoNaoEncontradoException;
import com.claro.desafio.pedidos.service.exception.TransicaoInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final Long USUARIO_1 = 1L;
    private static final Long USUARIO_2 = 2L;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pedidoService, "limiteMaximo", 5);
    }

    @Test
    void deveCriarPedidoQuandoAbaixoDoLimite() {
        when(pedidoRepository.countByUsuarioId(USUARIO_1)).thenReturn(4L);
        PedidoRequest request = new PedidoRequest("Cliente Teste", 2, 1000L);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(5L);
            return p;
        });

        Pedido criado = pedidoService.criar(request, USUARIO_1);

        assertThat(criado.getId()).isEqualTo(5L);
        assertThat(criado.getUsuarioId()).isEqualTo(USUARIO_1);
        assertThat(criado.getStatus()).isEqualTo(StatusPedido.EM_PROCESSAMENTO);
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    void naoDeveCriarPedidoQuandoLimiteAtingido() {
        when(pedidoRepository.countByUsuarioId(USUARIO_1)).thenReturn(5L);
        PedidoRequest request = new PedidoRequest("Cliente Teste", 2, 1000L);

        assertThatThrownBy(() -> pedidoService.criar(request, USUARIO_1))
                .isInstanceOf(LimiteExcedidoException.class);

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void limiteDeveSerPorUsuarioNaoGlobal() {
        // usuario 1 tem 5 pedidos (no limite), mas usuario 2 nao tem nenhum: deve poder criar
        when(pedidoRepository.countByUsuarioId(USUARIO_2)).thenReturn(0L);
        PedidoRequest request = new PedidoRequest("Cliente Teste", 2, 1000L);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido criado = pedidoService.criar(request, USUARIO_2);

        assertThat(criado.getUsuarioId()).isEqualTo(USUARIO_2);
        verify(pedidoRepository, never()).countByUsuarioId(USUARIO_1);
    }

    @Test
    void devePermitirTransicaoValida() {
        Pedido pedido = new Pedido(1L, "Cliente", 1, 1000L, StatusPedido.EM_PROCESSAMENTO, USUARIO_1);
        when(pedidoRepository.findByIdAndUsuarioId(1L, USUARIO_1)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido atualizado = pedidoService.alterarStatus(1L, StatusPedido.PAUSADO, USUARIO_1);

        assertThat(atualizado.getStatus()).isEqualTo(StatusPedido.PAUSADO);
    }

    @Test
    void naoDevePermitirTransicaoInvalida() {
        Pedido pedido = new Pedido(3L, "Cliente", 1, 1000L, StatusPedido.CANCELADO, USUARIO_1);
        when(pedidoRepository.findByIdAndUsuarioId(3L, USUARIO_1)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.alterarStatus(3L, StatusPedido.PAUSADO, USUARIO_1))
                .isInstanceOf(TransicaoInvalidaException.class);

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoAlterarStatusDePedidoInexistente() {
        when(pedidoRepository.findByIdAndUsuarioId(999L, USUARIO_1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.alterarStatus(999L, StatusPedido.PAUSADO, USUARIO_1))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }

    @Test
    void deveLancarNaoEncontradoAoAcessarPedidoDeOutroUsuario() {
        // pedido 1 existe (pertence ao usuario 1), mas usuario 2 tenta acessa-lo:
        // o repositorio (findByIdAndUsuarioId) simplesmente nao encontra nada para esse par id+usuario
        when(pedidoRepository.findByIdAndUsuarioId(1L, USUARIO_2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.alterarStatus(1L, StatusPedido.PAUSADO, USUARIO_2))
                .isInstanceOf(PedidoNaoEncontradoException.class);
        assertThatThrownBy(() -> pedidoService.excluir(1L, USUARIO_2))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }

    @Test
    void deveExcluirPedidoExistente() {
        Pedido pedido = new Pedido(1L, "Cliente", 1, 1000L, StatusPedido.EM_PROCESSAMENTO, USUARIO_1);
        when(pedidoRepository.findByIdAndUsuarioId(1L, USUARIO_1)).thenReturn(Optional.of(pedido));

        pedidoService.excluir(1L, USUARIO_1);

        verify(pedidoRepository).deleteById(1L);
    }

    @Test
    void deveLancarExcecaoAoExcluirPedidoInexistente() {
        when(pedidoRepository.findByIdAndUsuarioId(999L, USUARIO_1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.excluir(999L, USUARIO_1))
                .isInstanceOf(PedidoNaoEncontradoException.class);

        verify(pedidoRepository, never()).deleteById(any());
    }
}
