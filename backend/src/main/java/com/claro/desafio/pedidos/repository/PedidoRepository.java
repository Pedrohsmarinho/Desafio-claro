package com.claro.desafio.pedidos.repository;

import com.claro.desafio.pedidos.domain.Pedido;
import com.claro.desafio.pedidos.domain.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioId(Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    @Query("select p from Pedido p where p.usuarioId = :usuarioId "
            + "and (:status is null or p.status = :status) "
            + "and (:busca is null or lower(p.displayName) like lower(concat('%', :busca, '%')))")
    Page<Pedido> buscar(
            @Param("usuarioId") Long usuarioId,
            @Param("status") StatusPedido status,
            @Param("busca") String busca,
            Pageable pageable);

    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioIdAndStatus(Long usuarioId, StatusPedido status);

    long countByStatus(StatusPedido status);

    @Query("select coalesce(sum(p.peso), 0) from Pedido p")
    long somaPeso();

    @Query("select coalesce(sum(p.itens), 0) from Pedido p")
    long somaItens();
}
