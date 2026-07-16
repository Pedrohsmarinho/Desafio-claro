package com.claro.desafio.pedidos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Emissao e validacao de JWT. O token carrega apenas o email do usuario
 * (claim "sub") e a expiracao ("exp") - nenhum dado sensivel ou mutavel
 * (ex: senha, nome) e incluido no payload, que e apenas assinado (nao
 * criptografado) e portanto legivel por quem tiver o token.
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(
            @Value("${app.security.jwt-secret}") String segredo,
            @Value("${app.security.jwt-expiration-ms}") long expiracaoMs) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(String email) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    /** @return o email (claim "sub") se o token for valido e nao expirado; vazio caso contrario. */
    public Optional<String> validarEExtrairEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
