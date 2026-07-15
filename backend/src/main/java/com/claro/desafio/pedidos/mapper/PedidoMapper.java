package com.claro.desafio.pedidos.mapper;

import com.claro.desafio.pedidos.domain.Pedido;
import com.claro.desafio.pedidos.dto.PedidoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PedidoMapper {

    @Mapping(target = "pesoKg", expression = "java(pedido.getPeso() / 1000.0)")
    PedidoResponse toResponse(Pedido pedido);
}
