package com.claro.desafio.pedidos.service.exception;

public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(String message) {
        super(message);
    }
}
