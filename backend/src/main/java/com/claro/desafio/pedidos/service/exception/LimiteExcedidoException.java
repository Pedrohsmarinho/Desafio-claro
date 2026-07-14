package com.claro.desafio.pedidos.service.exception;

public class LimiteExcedidoException extends RuntimeException {
    public LimiteExcedidoException(String message) {
        super(message);
    }
}
