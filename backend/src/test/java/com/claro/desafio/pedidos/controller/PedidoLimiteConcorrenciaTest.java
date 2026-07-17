package com.claro.desafio.pedidos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoLimiteConcorrenciaTest {

    private static final int REQUISICOES_CONCORRENTES = 8;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requisicoesConcorrentesNoLimiteNaoDevemUltrapassarCincoPedidos() throws Exception {
        String token = registrarNovoUsuarioERetornarToken();
        for (int i = 0; i < 4; i++) {
            criarPedido(token, "Cliente existente " + i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(REQUISICOES_CONCORRENTES);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();

        for (int i = 0; i < REQUISICOES_CONCORRENTES; i++) {
            int indice = i;
            futuros.add(executor.submit(() -> {
                largada.await();
                return tentarCriarPedido(token, "Cliente concorrente " + indice);
            }));
        }
        largada.countDown();

        List<Integer> statusCodes = new ArrayList<>();
        for (Future<Integer> futuro : futuros) {
            statusCodes.add(futuro.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        long sucessos = statusCodes.stream().filter(codigo -> codigo == 201).count();
        long limitesExcedidos = statusCodes.stream().filter(codigo -> codigo == 422).count();

        assertThat(sucessos).isEqualTo(1);
        assertThat(limitesExcedidos).isEqualTo(REQUISICOES_CONCORRENTES - 1);
        assertThat(statusCodes).allMatch(codigo -> codigo == 201 || codigo == 422);

        mockMvc.perform(get("/api/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    private String registrarNovoUsuarioERetornarToken() throws Exception {
        String email = "usuario-limite-" + UUID.randomUUID() + "@teste.com";
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

    private void criarPedido(String token, String cliente) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "displayName", cliente,
                "itens", 1,
                "peso", 500));

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private int tentarCriarPedido(String token, String cliente) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "displayName", cliente,
                "itens", 1,
                "peso", 500));

        return mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();
    }
}
