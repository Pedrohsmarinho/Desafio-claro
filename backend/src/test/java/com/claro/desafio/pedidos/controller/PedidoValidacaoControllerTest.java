package com.claro.desafio.pedidos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validacao de entrada em POST /api/pedidos (Bean Validation) e rotas
 * inexistentes - contexto Spring completo (MockMvc, sem mocks).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PedidoValidacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String email = "usuario-validacao-" + UUID.randomUUID() + "@teste.com";
        String response = mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Usuario de Teste",
                                "email", email,
                                "senha", "senha12345"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void clienteVazioRetorna400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("displayName", "", "itens", 1, "peso", 500))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void clienteComMenosDeCincoCaracteresRetorna400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("displayName", "Ana", "itens", 1, "peso", 500))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("displayName")));
    }

    @Test
    void pesoAusenteRetorna400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("displayName", "Cliente Valido", "itens", 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void itensAusenteRetorna400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("displayName", "Cliente Valido", "peso", 500))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pesoNegativoRetorna400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("displayName", "Cliente Valido", "itens", 1, "peso", -100))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void itensNegativoRetorna400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("displayName", "Cliente Valido", "itens", -1, "peso", 500))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pesoNaoNumericoRetorna400NaoQuebraCom500() throws Exception {
        String jsonComPesoTexto = "{\"displayName\":\"Cliente Valido\",\"itens\":1,\"peso\":\"abc\"}";

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonComPesoTexto))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rotaInexistenteRetorna404ComFormatoJsonPadronizado() throws Exception {
        mockMvc.perform(get("/api/rota-que-nao-existe").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    void idComTipoInvalidoAoExcluirRetorna400NaoQuebraCom500() throws Exception {
        // DELETE /api/pedidos/{id} espera um Long - "abc" dispara
        // MethodArgumentTypeMismatchException, que sem handler dedicado cai
        // no catch-all generico (500) em vez do 400 esperado
        mockMvc.perform(delete("/api/pedidos/abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("id")));
    }

    @Test
    void idComTipoInvalidoAoAlterarStatusRetorna400NaoQuebraCom500() throws Exception {
        mockMvc.perform(patch("/api/pedidos/abc/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PAUSADO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("id")));
    }

    @Test
    void metodoHttpNaoSuportadoParaOPathRetorna405NaoQuebraCom500() throws Exception {
        // "/api/pedidos/abc" casa com o padrao de path de DELETE/PATCH
        // /api/pedidos/{id}, mas nao ha nenhum GET /api/pedidos/{id} - o Spring
        // lanca HttpRequestMethodNotSupportedException, que sem handler
        // dedicado cai no catch-all generico (500) em vez do 405 esperado
        mockMvc.perform(get("/api/pedidos/abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
