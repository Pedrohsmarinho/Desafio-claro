package com.claro.desafio.pedidos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank(message = "Nome e obrigatorio")
        @Size(min = 3, message = "Nome deve ter ao menos 3 caracteres")
        String nome,

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres")
        String senha
) {
}
