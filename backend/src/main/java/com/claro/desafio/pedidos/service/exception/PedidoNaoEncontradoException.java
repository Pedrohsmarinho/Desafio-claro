package com.claro.desafio.pedidos.service.exception;

public class PedidoNaoEncontradoException extends RuntimeException {
    public PedidoNaoEncontradoException(Long id) {
        super("Pedido nao encontrado: id=" + id);
    }
}
