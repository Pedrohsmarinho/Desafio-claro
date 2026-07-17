package com.claro.desafio.pedidos.exception;

import com.claro.desafio.pedidos.dto.ErrorResponse;
import com.claro.desafio.pedidos.service.exception.CredenciaisInvalidasException;
import com.claro.desafio.pedidos.service.exception.EmailJaCadastradoException;
import com.claro.desafio.pedidos.service.exception.LimiteExcedidoException;
import com.claro.desafio.pedidos.service.exception.PedidoNaoEncontradoException;
import com.claro.desafio.pedidos.service.exception.TransicaoInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest requestFake(String metodo, String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(metodo);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }

    @Test
    void handleValidacaoRetorna400ComMensagensDosCamposInvalidos() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "pedidoRequest");
        bindingResult.addError(new FieldError("pedidoRequest", "displayName", "Nome do cliente deve ter ao menos 5 caracteres"));
        MethodParameter methodParameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> resposta = handler.handleValidacao(ex, requestFake("POST", "/api/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = resposta.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).contains("displayName").contains("Nome do cliente deve ter ao menos 5 caracteres");
        assertThat(body.path()).isEqualTo("/api/pedidos");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void handleCredenciaisInvalidasRetorna401() {
        var ex = new CredenciaisInvalidasException("Email ou senha invalidos");

        ResponseEntity<ErrorResponse> resposta = handler.handleCredenciaisInvalidas(ex, requestFake("POST", "/api/auth/login"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody().status()).isEqualTo(401);
        assertThat(resposta.getBody().error()).isEqualTo("Unauthorized");
        assertThat(resposta.getBody().message()).isEqualTo("Email ou senha invalidos");
    }

    @Test
    void handlePedidoNaoEncontradoRetorna404() {
        var ex = new PedidoNaoEncontradoException(42L);

        ResponseEntity<ErrorResponse> resposta = handler.handlePedidoNaoEncontrado(ex, requestFake("DELETE", "/api/pedidos/42"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().status()).isEqualTo(404);
        assertThat(resposta.getBody().message()).contains("id=42");
    }

    @Test
    void handleEmailJaCadastradoRetorna409() {
        var ex = new EmailJaCadastradoException("fulano@teste.com");

        ResponseEntity<ErrorResponse> resposta = handler.handleEmailJaCadastrado(ex, requestFake("POST", "/api/auth/registrar"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().status()).isEqualTo(409);
        assertThat(resposta.getBody().message()).contains("fulano@teste.com");
    }

    @Test
    void handleRegraDeNegocioComLimiteExcedidoRetorna422() {
        var ex = new LimiteExcedidoException("Limite maximo de 5 pedidos cadastrados foi atingido");

        ResponseEntity<ErrorResponse> resposta = handler.handleRegraDeNegocio(ex, requestFake("POST", "/api/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getBody().status()).isEqualTo(422);
        assertThat(resposta.getBody().message()).contains("Limite maximo");
    }

    @Test
    void handleRegraDeNegocioComTransicaoInvalidaRetorna422() {
        var ex = new TransicaoInvalidaException("Transicao invalida de CANCELADO para PAUSADO");

        ResponseEntity<ErrorResponse> resposta = handler.handleRegraDeNegocio(ex, requestFake("PATCH", "/api/pedidos/1/status"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getBody().status()).isEqualTo(422);
        assertThat(resposta.getBody().message()).contains("Transicao invalida");
    }

    @Test
    void handleRecursoNaoEncontradoRetorna404ComMetodoEPathNaMensagem() {
        var ex = new NoResourceFoundException(HttpMethod.GET, "rota-que-nao-existe");

        ResponseEntity<ErrorResponse> resposta = handler.handleRecursoNaoEncontrado(ex, requestFake("GET", "/api/rota-que-nao-existe"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().status()).isEqualTo(404);
        assertThat(resposta.getBody().message()).contains("GET").contains("rota-que-nao-existe");
    }

    @Test
    void handleCorpoInvalidoRetorna400SemVazarDetalheDeParsingInterno() {
        var ex = new HttpMessageNotReadableException("JSON parse error: Cannot deserialize value of type `java.lang.Long`");

        ResponseEntity<ErrorResponse> resposta = handler.handleCorpoInvalido(ex, requestFake("POST", "/api/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().status()).isEqualTo(400);
        assertThat(resposta.getBody().message()).isEqualTo("Corpo da requisicao invalido ou mal formatado");
        assertThat(resposta.getBody().message()).doesNotContain("JSON parse error").doesNotContain("java.lang.Long");
    }

    @Test
    void handleTipoInvalidoRetorna400ParaPathVariableComTipoIncompativel() {
        MethodParameter methodParameter = mock(MethodParameter.class);
        var ex = new MethodArgumentTypeMismatchException("abc", Long.class, "id", methodParameter, new NumberFormatException());

        ResponseEntity<ErrorResponse> resposta = handler.handleTipoInvalido(ex, requestFake("GET", "/api/pedidos/abc"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().status()).isEqualTo(400);
        assertThat(resposta.getBody().message()).contains("id");
    }

    @Test
    void handleViolacaoDeIntegridadeRetorna409SemVazarDetalheDeConstraintDoBanco() {
        var ex = new DataIntegrityViolationException("could not execute statement; SQL [n/a]; constraint [uk_usuarios_email]");

        ResponseEntity<ErrorResponse> resposta = handler.handleViolacaoDeIntegridade(ex, requestFake("POST", "/api/auth/registrar"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().status()).isEqualTo(409);
        assertThat(resposta.getBody().message()).isEqualTo("Ja existe uma conta cadastrada com este email");
        assertThat(resposta.getBody().message()).doesNotContain("uk_usuarios_email").doesNotContain("SQL");
    }

    @Test
    void handleInesperadoCapturaExcecaoGenericaNaoMapeadaERetorna500SemVazarStacktrace() {
        var ex = new RuntimeException("Falha inesperada de infraestrutura - detalhe sensivel de banco");

        ResponseEntity<ErrorResponse> resposta = handler.handleInesperado(ex, requestFake("POST", "/api/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().status()).isEqualTo(500);
        assertThat(resposta.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(resposta.getBody().message()).isEqualTo("Erro interno inesperado");
        assertThat(resposta.getBody().message())
                .doesNotContain("RuntimeException")
                .doesNotContain("infraestrutura")
                .doesNotContain("detalhe sensivel");
    }

    @Test
    void handleInesperadoTambemCapturaNullPointerExceptionSemSerUmaDasCustomizadas() {
        var ex = new NullPointerException("algum campo nulo inesperado internamente");

        ResponseEntity<ErrorResponse> resposta = handler.handleInesperado(ex, requestFake("GET", "/api/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().message()).isEqualTo("Erro interno inesperado");
        assertThat(resposta.getBody().message()).doesNotContain("NullPointerException").doesNotContain("campo nulo");
    }
}
