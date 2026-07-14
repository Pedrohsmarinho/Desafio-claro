package com.claro.desafio.pedidos.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-tamanho-suficiente-para-hs256-0123456789";

    @Test
    void deveGerarTokenEExtrairEmailDeVolta() {
        JwtService jwtService = new JwtService(SEGREDO, 3_600_000);

        String token = jwtService.gerarToken("usuario@teste.com");

        assertThat(jwtService.validarEExtrairEmail(token)).contains("usuario@teste.com");
    }

    @Test
    void deveRetornarVazioParaTokenExpirado() throws InterruptedException {
        JwtService jwtService = new JwtService(SEGREDO, 1);

        String token = jwtService.gerarToken("usuario@teste.com");
        Thread.sleep(5);

        assertThat(jwtService.validarEExtrairEmail(token)).isEmpty();
    }

    @Test
    void deveRetornarVazioParaTokenInvalido() {
        JwtService jwtService = new JwtService(SEGREDO, 3_600_000);

        assertThat(jwtService.validarEExtrairEmail("token-completamente-invalido")).isEmpty();
    }

    @Test
    void naoDeveValidarTokenAssinadoComOutroSegredo() {
        JwtService emissor = new JwtService(SEGREDO, 3_600_000);
        JwtService validador = new JwtService("outro-segredo-completamente-diferente-0123456789abcdef", 3_600_000);

        String token = emissor.gerarToken("usuario@teste.com");

        assertThat(validador.validarEExtrairEmail(token)).isEmpty();
    }
}
