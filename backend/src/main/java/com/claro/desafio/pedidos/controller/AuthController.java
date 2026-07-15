package com.claro.desafio.pedidos.controller;

import com.claro.desafio.pedidos.dto.LoginRequest;
import com.claro.desafio.pedidos.dto.LoginResponse;
import com.claro.desafio.pedidos.dto.RegistroRequest;
import com.claro.desafio.pedidos.mapper.UsuarioMapper;
import com.claro.desafio.pedidos.service.AuthService;
import com.claro.desafio.pedidos.service.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Cadastro e login de usuarios")
public class AuthController {

    private final AuthService authService;
    private final UsuarioMapper usuarioMapper;

    @PostMapping("/login")
    @Operation(summary = "Autentica o usuario com email/senha e retorna um JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciais validas"),
            @ApiResponse(responseCode = "400", description = "Email ou senha ausentes/invalidos (formato)"),
            @ApiResponse(responseCode = "401", description = "Email ou senha incorretos"),
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UsuarioAutenticado resultado = authService.login(request);
        return ResponseEntity.ok(usuarioMapper.toLoginResponse(resultado.usuario(), resultado.token()));
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo usuario e ja retorna um JWT (login automatico)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos (nome, email ou senha fora das regras)"),
            @ApiResponse(responseCode = "409", description = "Ja existe uma conta com esse email"),
    })
    public LoginResponse registrar(@Valid @RequestBody RegistroRequest request) {
        UsuarioAutenticado resultado = authService.registrar(request);
        return usuarioMapper.toLoginResponse(resultado.usuario(), resultado.token());
    }
}
