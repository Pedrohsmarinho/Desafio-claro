package com.claro.desafio.pedidos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pedido de e-commerce. O peso e armazenado sempre em gramas; a conversao
 * para quilogramas fica a cargo da camada de apresentacao (DTO/frontend).
 */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private Integer itens;

    /** Peso do pedido em gramas. */
    @Column(nullable = false)
    private Long peso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    /**
     * Dono do pedido. Nunca e preenchido a partir de dado vindo do cliente
     * (corpo/query da requisicao) - sempre a partir do usuario autenticado
     * extraido do JWT (ver PedidoController/JwtAuthenticationFilter).
     */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
}
