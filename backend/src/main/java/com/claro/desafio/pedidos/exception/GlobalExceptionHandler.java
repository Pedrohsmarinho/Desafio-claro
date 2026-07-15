package com.claro.desafio.pedidos.exception;

import com.claro.desafio.pedidos.dto.ErrorResponse;
import com.claro.desafio.pedidos.service.exception.CredenciaisInvalidasException;
import com.claro.desafio.pedidos.service.exception.EmailJaCadastradoException;
import com.claro.desafio.pedidos.service.exception.LimiteExcedidoException;
import com.claro.desafio.pedidos.service.exception.PedidoNaoEncontradoException;
import com.claro.desafio.pedidos.service.exception.TransicaoInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredenciaisInvalidas(CredenciaisInvalidasException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handlePedidoNaoEncontrado(PedidoNaoEncontradoException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErrorResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler({LimiteExcedidoException.class, TransicaoInvalidaException.class})
    public ResponseEntity<ErrorResponse> handleRegraDeNegocio(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    /**
     * Rota que nao existe (nenhum @RequestMapping casa com o path/metodo).
     * Depende de spring.mvc.throw-exception-if-no-handler-found e
     * spring.web.resources.add-mappings=false (application.yml) - sem isso,
     * cairia no tratamento padrao de recurso estatico do Spring (Whitelabel
     * Error Page em HTML) em vez do mesmo formato JSON usado pelos outros
     * 404 da API.
     *
     * NoResourceFoundException e o que o Spring Framework 6.1+/Boot 3.2+ de
     * fato lanca nesse cenario (nao o mais antigo NoHandlerFoundException) -
     * descoberto empiricamente ao escrever o teste
     * rotaInexistenteRetorna404ComFormatoJsonPadronizado, que sem esse handler
     * especifico caia no catch-all generico (500) em vez do 404 esperado.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNaoEncontrado(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Rota nao encontrada: " + ex.getHttpMethod() + " " + ex.getResourcePath(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCorpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido ou mal formatado", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado ao processar {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
