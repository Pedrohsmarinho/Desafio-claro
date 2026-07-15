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

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/pedidos/busca: filtro (status/nome), paginacao e ordenacao
 * resolvidos no banco - a listagem do frontend passou a chamar esse
 * endpoint a cada mudanca de filtro/pagina/ordenacao, em vez de carregar
 * tudo uma vez e filtrar no navegador (ver PedidoListComponent).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PedidoBuscaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String email = "usuario-busca-" + UUID.randomUUID() + "@teste.com";
        String body = objectMapper.writeValueAsString(Map.of(
                "nome", "Usuario de Teste",
                "email", email,
                "senha", "senha12345"));

        String response = mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(response).get("token").asText();

        criarPedido("Pedido Ana Paula", 2, 1000L);
        criarPedido("Pedido Bruno Costa", 1, 500L);
        criarPedido("Pedido Carla Dias", 3, 2000L);

        long idBruno = idPorNome("Pedido Bruno Costa");
        mockMvc.perform(patch("/api/pedidos/{id}/status", idBruno)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PAUSADO"))))
                .andExpect(status().isOk());
    }

    @Test
    void semFiltroRetornaTodosOsPedidosDoUsuario() throws Exception {
        mockMvc.perform(get("/api/pedidos/busca").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    void filtraPorStatus() throws Exception {
        mockMvc.perform(get("/api/pedidos/busca?status=PAUSADO").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("Pedido Bruno Costa"));
    }

    @Test
    void buscaPorNomeContendoENaoSensivelACaixa() throws Exception {
        mockMvc.perform(get("/api/pedidos/busca?busca=CARLA").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("Pedido Carla Dias"));
    }

    @Test
    void combinaFiltroDeStatusEBusca() throws Exception {
        mockMvc.perform(get("/api/pedidos/busca?status=EM_PROCESSAMENTO&busca=ana")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("Pedido Ana Paula"));
    }

    @Test
    void naoEncontraNadaQuandoFiltroNaoCasaComNenhumPedido() throws Exception {
        mockMvc.perform(get("/api/pedidos/busca?busca=inexistente").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void paginaComTamanhoUmRetornaUmItemPorVez() throws Exception {
        mockMvc.perform(get("/api/pedidos/busca?size=1&page=0&sort=id,asc").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].displayName").value("Pedido Ana Paula"));

        mockMvc.perform(get("/api/pedidos/busca?size=1&page=2&sort=id,asc").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.content[0].displayName").value("Pedido Carla Dias"));
    }

    @Test
    void ordenaPorPesoDecrescente() throws Exception {
        mockMvc.perform(get("/api/pedidos/busca?sort=peso,desc").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].displayName").value(contains(
                        "Pedido Carla Dias", "Pedido Ana Paula", "Pedido Bruno Costa")));
    }

    @Test
    void naoRetornaPedidosDeOutroUsuario() throws Exception {
        String outroEmail = "usuario-busca-outro-" + UUID.randomUUID() + "@teste.com";
        String body = objectMapper.writeValueAsString(Map.of(
                "nome", "Outro Usuario",
                "email", outroEmail,
                "senha", "senha12345"));
        String response = mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String tokenOutroUsuario = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/pedidos/busca").header("Authorization", "Bearer " + tokenOutroUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void semTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/pedidos/busca"))
                .andExpect(status().isUnauthorized());
    }

    private void criarPedido(String cliente, int itens, long peso) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "displayName", cliente,
                "itens", itens,
                "peso", peso));

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private long idPorNome(String nome) throws Exception {
        String response = mockMvc.perform(get("/api/pedidos").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        var lista = objectMapper.readTree(response);
        for (var no : lista) {
            if (no.get("displayName").asText().equals(nome)) {
                return no.get("id").asLong();
            }
        }
        throw new IllegalStateException("Pedido nao encontrado: " + nome);
    }
}
