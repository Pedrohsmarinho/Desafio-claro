package com.claro.desafio.pedidos.service.exception;

public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException(String message) {
        super(message);
    }
}
