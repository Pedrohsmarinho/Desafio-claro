package com.claro.desafio.pedidos.controller;

import com.claro.desafio.pedidos.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirma que uma excecao NAO mapeada explicitamente (ex: uma RuntimeException
 * generica vinda de uma falha inesperada no service - banco fora do ar,
 * bug, etc.) cai no handler catch-all de GlobalExceptionHandler e retorna
 * 500 com o mesmo formato JSON padronizado dos outros erros, sem vazar a
 * stacktrace/nome da excecao para o cliente. PedidoService e substituido
 * por um mock (@MockitoBean) so nesta classe - o resto do contexto
 * (seguranca, AuthService, etc.) continua real, entao o 500 e disparado
 * atraves do fluxo HTTP de verdade, nao de uma chamada direta ao metodo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerErroInesperadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PedidoService pedidoService;

    @Test
    void erroInesperadoNoServiceRetorna500ComCorpoConsistenteSemVazarStacktrace() throws Exception {
        when(pedidoService.criar(any(), anyLong()))
                .thenThrow(new RuntimeException("Falha inesperada de infraestrutura - detalhe sensivel de banco"));

        String token = registrarNovoUsuarioERetornarToken();

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", "Cliente Valido",
                                "itens", 1,
                                "peso", 500))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Erro interno inesperado"))
                .andExpect(jsonPath("$.message", not(containsString("RuntimeException"))))
                .andExpect(jsonPath("$.message", not(containsString("infraestrutura"))));
    }

    private String registrarNovoUsuarioERetornarToken() throws Exception {
        String email = "usuario-500-" + UUID.randomUUID() + "@teste.com";
        String body = objectMapper.writeValueAsString(Map.of(
                "nome", "Usuario de Teste",
                "email", email,
                "senha", "senha12345"));

        String response = mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
}
