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

/** Peso sempre em gramas; conversao pra kg fica a cargo do frontend/DTO. */
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

    /** Sempre vem do usuario autenticado (JWT), nunca do corpo/query da requisicao. */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
}
