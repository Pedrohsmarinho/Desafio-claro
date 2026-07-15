package com.claro.desafio.pedidos.config;

import com.claro.desafio.pedidos.domain.Pedido;
import com.claro.desafio.pedidos.domain.StatusPedido;
import com.claro.desafio.pedidos.domain.Usuario;
import com.claro.desafio.pedidos.dto.PedidoRequest;
import com.claro.desafio.pedidos.repository.PedidoRepository;
import com.claro.desafio.pedidos.repository.UsuarioRepository;
import com.claro.desafio.pedidos.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria um usuario de demonstracao (mesmas credenciais documentadas no README
 * desde a fase obrigatoria do desafio) e, apenas na primeira subida, os 3
 * pedidos do seed do enunciado, associados a esse usuario.
 *
 * Os pedidos sao criados via PedidoService (nunca PedidoRepository
 * diretamente), passando pelas mesmas regras de negocio de qualquer pedido
 * real: limite maximo, status inicial sempre EM_PROCESSAMENTO (a mudanca
 * para PAUSADO/CANCELADO nos pedidos #2 e #3 acontece depois, via
 * alterarStatus, respeitando a mesma maquina de transicoes).
 *
 * @Profile("!test"): nao roda no profile de testes (ver
 * src/test/resources/application.yml) - nenhum teste depende dos dados
 * semeados, e alguns substituem PedidoService por mock (@MockitoBean) so
 * pra propria classe de teste, o que quebraria esse seed se ele rodasse ali.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String DEMO_EMAIL = "admin@pedidos.com";
    private static final String DEMO_SENHA = "admin123";

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;
    private final PedidoService pedidoService;

    @Override
    public void run(String... args) {
        Usuario demo = usuarioRepository.findByEmailIgnoreCase(DEMO_EMAIL)
                .orElseGet(this::criarUsuarioDemo);

        if (pedidoRepository.countByUsuarioId(demo.getId()) > 0) {
            return;
        }

        pedidoService.criar(new PedidoRequest("Pedido #1 - João Silva", 2, 1024L), demo.getId());

        Pedido pedido2 = pedidoService.criar(new PedidoRequest("Pedido #2 - Maria Souza", 1, 512L), demo.getId());
        pedidoService.alterarStatus(pedido2.getId(), StatusPedido.PAUSADO, demo.getId());

        Pedido pedido3 = pedidoService.criar(new PedidoRequest("Pedido #3 - Carlos Lima", 4, 2048L), demo.getId());
        pedidoService.alterarStatus(pedido3.getId(), StatusPedido.CANCELADO, demo.getId());

        log.info("Seed inicial de pedidos aplicado para o usuario de demonstracao '{}'", DEMO_EMAIL);
    }

    private Usuario criarUsuarioDemo() {
        Usuario usuario = new Usuario();
        usuario.setNome("Administrador");
        usuario.setEmail(DEMO_EMAIL);
        usuario.setSenhaHash(passwordEncoder.encode(DEMO_SENHA));
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuario de demonstracao criado: email='{}'", DEMO_EMAIL);
        return salvo;
    }
}
