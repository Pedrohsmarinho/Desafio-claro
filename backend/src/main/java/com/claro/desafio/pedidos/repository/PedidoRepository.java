package com.claro.desafio.pedidos.repository;

import com.claro.desafio.pedidos.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioId(Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    /** Usado para checar posse do pedido: um id que existe mas e de outro usuario deve "nao ser encontrado". */
    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);
}
