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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRegistroConcorrenciaTest {

    private static final int REQUISICOES_CONCORRENTES = 8;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requisicoesConcorrentesComMesmoEmailApenasUmaDeveSerCriada() throws Exception {
        String email = "concorrente-" + UUID.randomUUID() + "@teste.com";

        ExecutorService executor = Executors.newFixedThreadPool(REQUISICOES_CONCORRENTES);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();

        for (int i = 0; i < REQUISICOES_CONCORRENTES; i++) {
            futuros.add(executor.submit(() -> {
                largada.await();
                return tentarRegistrar(email);
            }));
        }
        largada.countDown();

        List<Integer> statusCodes = new ArrayList<>();
        for (Future<Integer> futuro : futuros) {
            statusCodes.add(futuro.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        long sucessos = statusCodes.stream().filter(codigo -> codigo == 201).count();
        long conflitos = statusCodes.stream().filter(codigo -> codigo == 409).count();

        assertThat(sucessos).isEqualTo(1);
        assertThat(conflitos).isEqualTo(REQUISICOES_CONCORRENTES - 1);
        assertThat(statusCodes).allMatch(codigo -> codigo == 201 || codigo == 409);
    }

    private int tentarRegistrar(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "nome", "Usuario Concorrente",
                "email", email,
                "senha", "senha12345"));

        return mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();
    }
}
