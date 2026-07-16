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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contexto Spring completo (MockMvc, sem mocks) cobrindo os caminhos de erro
 * de /api/auth - complementa AuthServiceTest (que testa a mesma logica
 * isolada, com o repositorio mockado) confirmando os codigos HTTP e o
 * formato de erro de verdade, atraves do GlobalExceptionHandler.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginComCredenciaisValidasRetorna200ComToken() throws Exception {
        String email = "login-sucesso-" + UUID.randomUUID() + "@teste.com";
        registrar(email, "senha12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", "senha12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginComSenhaIncorretaRetorna401() throws Exception {
        String email = "login-senha-errada-" + UUID.randomUUID() + "@teste.com";
        registrar(email, "senha12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", "senha-errada"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginComEmailInexistenteRetorna401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nao-existe-" + UUID.randomUUID() + "@teste.com", "senha", "qualquersenha"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginComEmailEmFormatoInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nao-e-um-email", "senha", "qualquersenha"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void loginSemSenhaRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "usuario@teste.com", "senha", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarComSucessoRetorna201ComToken() throws Exception {
        String email = "registro-sucesso-" + UUID.randomUUID() + "@teste.com";

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Usuario Teste",
                                "email", email,
                                "senha", "senha12345"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void registrarComEmailJaCadastradoRetorna409() throws Exception {
        String email = "registro-duplicado-" + UUID.randomUUID() + "@teste.com";
        registrar(email, "senha12345");

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Outro Nome",
                                "email", email,
                                "senha", "outrasenha"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void registrarComEmailEmFormatoInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Usuario Teste",
                                "email", "nao-e-um-email",
                                "senha", "senha12345"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarComSenhaCurtaRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Usuario Teste",
                                "email", "senha-curta-" + UUID.randomUUID() + "@teste.com",
                                "senha", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("senha")));
    }

    @Test
    void registrarComNomeCurtoRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Ab",
                                "email", "nome-curto-" + UUID.randomUUID() + "@teste.com",
                                "senha", "senha12345"))))
                .andExpect(status().isBadRequest());
    }

    private void registrar(String email, String senha) throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Usuario de Teste",
                                "email", email,
                                "senha", senha))))
                .andExpect(status().isCreated());
    }
}
