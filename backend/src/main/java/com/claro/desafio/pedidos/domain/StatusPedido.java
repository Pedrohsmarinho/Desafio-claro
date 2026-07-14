package com.claro.desafio.pedidos.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Status possiveis de um pedido e a maquina de estados que governa as
 * transicoes permitidas entre eles.
 *
 * Transicoes permitidas (De -> Para):
 *   EM_PROCESSAMENTO -> PAUSADO
 *   EM_PROCESSAMENTO -> CANCELADO
 *   PAUSADO          -> CANCELADO
 *   PAUSADO          -> EM_PROCESSAMENTO
 *   CANCELADO        -> EM_PROCESSAMENTO
 *
 * Qualquer transicao fora dessa tabela (incluindo CANCELADO -> PAUSADO ou
 * transicoes para o mesmo estado) e considerada invalida.
 */
public enum StatusPedido {
    EM_PROCESSAMENTO,
    PAUSADO,
    CANCELADO;

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES_PERMITIDAS = new EnumMap<>(StatusPedido.class);

    static {
        TRANSICOES_PERMITIDAS.put(EM_PROCESSAMENTO, EnumSet.of(PAUSADO, CANCELADO));
        TRANSICOES_PERMITIDAS.put(PAUSADO, EnumSet.of(CANCELADO, EM_PROCESSAMENTO));
        TRANSICOES_PERMITIDAS.put(CANCELADO, EnumSet.of(EM_PROCESSAMENTO));
    }

    public boolean podeTransicionarPara(StatusPedido novoStatus) {
        return TRANSICOES_PERMITIDAS.get(this).contains(novoStatus);
    }
}
