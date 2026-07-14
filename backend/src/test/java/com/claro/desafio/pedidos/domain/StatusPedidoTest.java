package com.claro.desafio.pedidos.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatusPedidoTest {

    @ParameterizedTest(name = "{0} -> {1} deve ser {2}")
    @CsvSource({
            "EM_PROCESSAMENTO, PAUSADO, true",
            "EM_PROCESSAMENTO, CANCELADO, true",
            "EM_PROCESSAMENTO, EM_PROCESSAMENTO, false",
            "PAUSADO, CANCELADO, true",
            "PAUSADO, EM_PROCESSAMENTO, true",
            "PAUSADO, PAUSADO, false",
            "CANCELADO, EM_PROCESSAMENTO, true",
            "CANCELADO, PAUSADO, false",
            "CANCELADO, CANCELADO, false",
    })
    void deveValidarTransicoesPermitidas(StatusPedido origem, StatusPedido destino, boolean esperado) {
        assertThat(origem.podeTransicionarPara(destino)).isEqualTo(esperado);
    }
}
