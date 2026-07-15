package com.claro.desafio.pedidos.mapper;

import com.claro.desafio.pedidos.domain.Usuario;
import com.claro.desafio.pedidos.dto.LoginResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * so mapeia email (nunca senhaHash) pra LoginResponse - o token nao vem da
 * entidade, e gerado pelo JwtService e passado como segundo parametro.
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "email", source = "usuario.email")
    @Mapping(target = "token", source = "token")
    LoginResponse toLoginResponse(Usuario usuario, String token);
}
