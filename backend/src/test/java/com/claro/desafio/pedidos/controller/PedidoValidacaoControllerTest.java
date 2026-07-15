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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("displayName")));
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
}
