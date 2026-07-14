package com.claro.desafio.pedidos.service;

import com.claro.desafio.pedidos.domain.Usuario;
import com.claro.desafio.pedidos.dto.LoginRequest;
import com.claro.desafio.pedidos.dto.LoginResponse;
import com.claro.desafio.pedidos.dto.RegistroRequest;
import com.claro.desafio.pedidos.repository.UsuarioRepository;
import com.claro.desafio.pedidos.security.JwtService;
import com.claro.desafio.pedidos.service.exception.CredenciaisInvalidasException;
import com.claro.desafio.pedidos.service.exception.EmailJaCadastradoException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email())
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                .orElse(null);

        if (usuario == null) {
            log.warn("Tentativa de login falhou para email='{}'", request.email());
            throw new CredenciaisInvalidasException("Email ou senha invalidos");
        }

        log.info("Login realizado com sucesso: email='{}'", usuario.getEmail());
        return new LoginResponse(usuario.getEmail(), jwtService.gerarToken(usuario.getEmail()));
    }

    /** Cadastra um novo usuario e ja autentica (retorna token), evitando um passo extra de login. */
    public LoginResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            log.warn("Tentativa de cadastro com email ja existente: '{}'", request.email());
            throw new EmailJaCadastradoException(request.email());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuarioRepository.save(usuario);

        log.info("Usuario cadastrado: email='{}'", usuario.getEmail());
        return new LoginResponse(usuario.getEmail(), jwtService.gerarToken(usuario.getEmail()));
    }
}
