package com.claro.desafio.pedidos.service;

import com.claro.desafio.pedidos.domain.Usuario;
import com.claro.desafio.pedidos.dto.LoginRequest;
import com.claro.desafio.pedidos.dto.RegistroRequest;
import com.claro.desafio.pedidos.repository.UsuarioRepository;
import com.claro.desafio.pedidos.security.JwtService;
import com.claro.desafio.pedidos.service.exception.CredenciaisInvalidasException;
import com.claro.desafio.pedidos.service.exception.EmailJaCadastradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void deveRegistrarNovoUsuarioEJaAutenticar() {
        RegistroRequest request = new RegistroRequest("Fulano de Tal", "fulano@teste.com", "senha1234");
        when(usuarioRepository.existsByEmailIgnoreCase("fulano@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("hash-fake");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.gerarToken("fulano@teste.com")).thenReturn("jwt-fake");

        UsuarioAutenticado resultado = authService.registrar(request);

        assertThat(resultado.usuario().getEmail()).isEqualTo("fulano@teste.com");
        assertThat(resultado.token()).isEqualTo("jwt-fake");
        verify(usuarioRepository).save(argThat(u -> u.getSenhaHash().equals("hash-fake")));
    }

    @Test
    void naoDeveRegistrarComEmailJaExistente() {
        RegistroRequest request = new RegistroRequest("Fulano de Tal", "fulano@teste.com", "senha1234");
        when(usuarioRepository.existsByEmailIgnoreCase("fulano@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(EmailJaCadastradoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveLogarComSucessoQuandoCredenciaisCorretas() {
        Usuario usuario = new Usuario(1L, "Fulano", "fulano@teste.com", "hash-fake");
        when(usuarioRepository.findByEmailIgnoreCase("fulano@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha1234", "hash-fake")).thenReturn(true);
        when(jwtService.gerarToken("fulano@teste.com")).thenReturn("jwt-fake");

        UsuarioAutenticado resultado = authService.login(new LoginRequest("fulano@teste.com", "senha1234"));

        assertThat(resultado.token()).isEqualTo("jwt-fake");
        assertThat(resultado.usuario().getEmail()).isEqualTo("fulano@teste.com");
    }

    @Test
    void naoDeveLogarComSenhaIncorreta() {
        Usuario usuario = new Usuario(1L, "Fulano", "fulano@teste.com", "hash-fake");
        when(usuarioRepository.findByEmailIgnoreCase("fulano@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("fulano@teste.com", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void naoDeveLogarComEmailInexistente() {
        when(usuarioRepository.findByEmailIgnoreCase("inexistente@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("inexistente@teste.com", "qualquer")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }
}
