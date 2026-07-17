package com.claro.desafio.pedidos.controller;

import com.claro.desafio.pedidos.domain.Pedido;
import com.claro.desafio.pedidos.domain.StatusPedido;
import com.claro.desafio.pedidos.domain.Usuario;
import com.claro.desafio.pedidos.dto.PedidoRequest;
import com.claro.desafio.pedidos.dto.PedidoResponse;
import com.claro.desafio.pedidos.dto.PedidoStatusUpdateRequest;
import com.claro.desafio.pedidos.mapper.PedidoMapper;
import com.claro.desafio.pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Dono dos pedidos vem sempre do principal autenticado (JWT), nunca de parametro da requisicao. */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Cadastro, consulta, mudanca de status e exclusao de pedidos do usuario autenticado")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PedidoMapper pedidoMapper;

    @GetMapping
    @Operation(summary = "Lista os pedidos do usuario autenticado")
    public List<PedidoResponse> listar(@AuthenticationPrincipal Usuario usuario) {
        return pedidoService.listarTodos(usuario.getId()).stream()
                .map(pedidoMapper::toResponse)
                .toList();
    }

    @GetMapping("/busca")
    @Operation(summary = "Lista os pedidos do usuario autenticado com filtro, paginacao e ordenacao resolvidos no banco")
    public Page<PedidoResponse> buscar(
            @Parameter(description = "Filtra por status exato; omitido retorna todos os status") @RequestParam(required = false) StatusPedido status,
            @Parameter(description = "Filtra por nome do cliente (contem, sem diferenciar maiusculas/minusculas); omitido nao filtra") @RequestParam(required = false) String busca,
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @AuthenticationPrincipal Usuario usuario) {
        return pedidoService.buscar(usuario.getId(), status, busca, pageable).map(pedidoMapper::toResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo pedido para o usuario autenticado (status inicial sempre EM_PROCESSAMENTO)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos (ex: nome com menos de 5 caracteres)"),
            @ApiResponse(responseCode = "422", description = "Limite maximo de 5 pedidos (do usuario) foi atingido"),
    })
    public PedidoResponse criar(@Valid @RequestBody PedidoRequest request, @AuthenticationPrincipal Usuario usuario) {
        Pedido pedido = pedidoService.criar(request, usuario.getId());
        return pedidoMapper.toResponse(pedido);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera o status de um pedido do usuario autenticado, respeitando a maquina de transicoes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status alterado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pedido nao encontrado (ou pertence a outro usuario)"),
            @ApiResponse(responseCode = "422", description = "Transicao de status invalida para o estado atual"),
    })
    public PedidoResponse alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody PedidoStatusUpdateRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        Pedido pedido = pedidoService.alterarStatus(id, request.status(), usuario.getId());
        return pedidoMapper.toResponse(pedido);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um pedido do usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pedido excluido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pedido nao encontrado (ou pertence a outro usuario)"),
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        pedidoService.excluir(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
