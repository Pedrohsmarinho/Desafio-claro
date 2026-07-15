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

    /**
     * Filtro combinado (status e busca por nome sao opcionais, aplicados
     * somente quando informados) com paginacao/ordenacao delegadas ao
     * Pageable - usado pela listagem paginada da API (GET /api/pedidos/busca).
     */
    @Query("select p from Pedido p where p.usuarioId = :usuarioId "
            + "and (:status is null or p.status = :status) "
            + "and (:busca is null or lower(p.displayName) like lower(concat('%', :busca, '%')))")
    Page<Pedido> buscar(
            @Param("usuarioId") Long usuarioId,
            @Param("status") StatusPedido status,
            @Param("busca") String busca,
            Pageable pageable);

    /** Usado para checar posse do pedido: um id que existe mas e de outro usuario deve "nao ser encontrado". */
    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Usado nas metricas de negocio (pedidos_by_status) - contagem global, entre todos os usuarios. */
    long countByStatus(StatusPedido status);

    /** Usado na metrica de negocio pedidos_peso_total_gramas - soma global, entre todos os usuarios. */
    @Query("select coalesce(sum(p.peso), 0) from Pedido p")
    long somaPeso();

    /** Usado na metrica de negocio pedidos_itens_total - soma global, entre todos os usuarios. */
    @Query("select coalesce(sum(p.itens), 0) from Pedido p")
    long somaItens();
}
