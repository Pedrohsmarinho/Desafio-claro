package com.claro.desafio.pedidos.controller;

import com.claro.desafio.pedidos.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de ponta a ponta (contexto Spring completo + filtro de seguranca
 * real, nao mockado) confirmando que /api/pedidos exige um JWT valido em
 * todas as operacoes, e que as regras de negocio (limite, transicao,
 * autorizacao entre usuarios) respondem com os codigos HTTP corretos quando
 * acionadas atraves da API de verdade - complementando os testes unitarios
 * de PedidoServiceTest, que cobrem a mesma logica isolada do HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.security.jwt-secret}")
    private String jwtSecret;

    @Test
    void semTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", "Bearer token-completamente-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoDeveRetornar401() throws Exception {
        JwtService jwtServiceComExpiracaoCurta = new JwtService(jwtSecret, 1);
        String tokenExpirado = jwtServiceComExpiracaoCurta.gerarToken("admin@pedidos.com");
        Thread.sleep(5);

        mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenExpirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoDeveRetornar200EListarApenasPedidosDoProprioUsuario() throws Exception {
        String token = registrarNovoUsuarioERetornarToken();

        mockMvc.perform(get("/api/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void transicaoDeStatusInvalidaDeveRetornar422() throws Exception {
        String token = registrarNovoUsuarioERetornarToken();
        long pedidoId = criarPedido(token, "Cliente Teste");

        // EM_PROCESSAMENTO so pode ir para PAUSADO ou CANCELADO, nunca direto para CANCELADO -> PAUSADO de novo
        mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELADO"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PAUSADO"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void limiteDeCincoPedidosPorUsuarioDeveRetornar422() throws Exception {
        String token = registrarNovoUsuarioERetornarToken();

        for (int i = 0; i < 5; i++) {
            criarPedido(token, "Cliente Numero " + i);
        }

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", "Cliente Sexto Pedido",
                                "itens", 1,
                                "peso", 500))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void naoDevePermitirAcessoAPedidoDeOutroUsuario() throws Exception {
        String tokenUsuario1 = registrarNovoUsuarioERetornarToken();
        long pedidoDoUsuario1 = criarPedido(tokenUsuario1, "Pedido do usuario 1");

        String tokenUsuario2 = registrarNovoUsuarioERetornarToken();

        mockMvc.perform(patch("/api/pedidos/{id}/status", pedidoDoUsuario1)
                        .header("Authorization", "Bearer " + tokenUsuario2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PAUSADO"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/pedidos/{id}", pedidoDoUsuario1).header("Authorization", "Bearer " + tokenUsuario2))
                .andExpect(status().isNotFound());
    }

    private String registrarNovoUsuarioERetornarToken() throws Exception {
        String email = "usuario-" + UUID.randomUUID() + "@teste.com";
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
