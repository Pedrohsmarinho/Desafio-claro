package com.claro.desafio.pedidos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PedidoRequest(

        @NotBlank(message = "Nome do cliente e obrigatorio")
        @Size(min = 5, message = "Nome do cliente deve ter ao menos 5 caracteres")
        String displayName,

        @NotNull(message = "Quantidade de itens e obrigatoria")
        @Min(value = 1, message = "Quantidade de itens deve ser maior que zero")
        Integer itens,

        @NotNull(message = "Peso e obrigatorio")
        @Min(value = 1, message = "Peso deve ser maior que zero")
        Long peso
) {
}
