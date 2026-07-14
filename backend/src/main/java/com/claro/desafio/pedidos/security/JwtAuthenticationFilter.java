package com.claro.desafio.pedidos.security;

import com.claro.desafio.pedidos.domain.Usuario;
import com.claro.desafio.pedidos.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Le o header Authorization: Bearer <token>, valida o JWT e, se valido,
 * carrega o Usuario correspondente e o coloca no SecurityContext como
 * principal - e a partir dai que os controllers descobrem "quem" fez a
 * requisicao (nunca de um usuarioId vindo do corpo/query da requisicao).
 * Token ausente/invalido/expirado simplesmente deixa o contexto vazio; quem
 * decide se isso vira 401 e a regra de autorizacao no SecurityConfig.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length());

            Optional<Usuario> usuario = jwtService.validarEExtrairEmail(token)
                    .flatMap(usuarioRepository::findByEmailIgnoreCase);

            usuario.ifPresent(u -> {
                var authentication = new UsernamePasswordAuthenticationToken(u, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }
}
