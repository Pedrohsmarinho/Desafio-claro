package com.claro.desafio.pedidos.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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
