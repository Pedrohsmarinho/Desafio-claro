package com.claro.desafio.pedidos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/dashboard/metricas: metricas do usuario autenticado (cards e
 * graficos do dashboard do frontend) - consulta o banco escopada por
 * usuarioId, deliberadamente distinta das metricas globais de
 * /actuator/prometheus (ver PedidoService.buscarMetricasDashboard e o
 * README para a justificativa).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void retornaZeroPedidosParaUsuarioRecemCriado() throws Exception {
        String token = registrarNovoUsuarioERetornarToken();

        mockMvc.perform(get("/api/dashboard/metricas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(0))
                .andExpect(jsonPath("$.porStatus.EM_PROCESSAMENTO").value(0))
                .andExpect(jsonPath("$.porStatus.PAUSADO").value(0))
                .andExpect(jsonPath("$.porStatus.CANCELADO").value(0))
                .andExpect(jsonPath("$.limiteMaximo").value(5));
    }

    @Test
    void contaPedidosDoUsuarioPorStatusCorretamente() throws Exception {
        String token = registrarNovoUsuarioERetornarToken();

        long id1 = criarPedido(token, "Pedido Um");
        criarPedido(token, "Pedido Dois");
        criarPedido(token, "Pedido Tres");

        mockMvc.perform(patch("/api/pedidos/{id}/status", id1)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELADO"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/metricas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(3))
                .andExpect(jsonPath("$.porStatus.EM_PROCESSAMENTO").value(2))
                .andExpect(jsonPath("$.porStatus.PAUSADO").value(0))
                .andExpect(jsonPath("$.porStatus.CANCELADO").value(1))
                .andExpect(jsonPath("$.limiteMaximo").value(5));
    }

    @Test
    void naoContaPedidosDeOutroUsuario() throws Exception {
        String tokenUsuario1 = registrarNovoUsuarioERetornarToken();
        criarPedido(tokenUsuario1, "Pedido do usuario 1");
        criarPedido(tokenUsuario1, "Outro pedido do usuario 1");

        String tokenUsuario2 = registrarNovoUsuarioERetornarToken();
        criarPedido(tokenUsuario2, "Pedido do usuario 2");

        mockMvc.perform(get("/api/dashboard/metricas").header("Authorization", "Bearer " + tokenUsuario2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(1));
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/dashboard/metricas"))
                .andExpect(status().isUnauthorized());
    }

    private String registrarNovoUsuarioERetornarToken() throws Exception {
        String email = "usuario-dashboard-" + UUID.randomUUID() + "@teste.com";
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

    private long criarPedido(String token, String cliente) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "displayName", cliente,
                "itens", 2,
                "peso", 1000));

        String response = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
