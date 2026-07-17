package com.claro.desafio.pedidos.mapper;

import com.claro.desafio.pedidos.domain.Usuario;
import com.claro.desafio.pedidos.dto.LoginResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** So mapeia email (nunca senhaHash); token vem do JwtService, nao da entidade. */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "email", source = "usuario.email")
    @Mapping(target = "token", source = "token")
    LoginResponse toLoginResponse(Usuario usuario, String token);
}
