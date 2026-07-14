package com.claro.desafio.pedidos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pedidosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pedidos API")
                        .description("API de gestao de pedidos de e-commerce - Desafio Tecnico Claro")
                        .version("1.0.0")
                        .contact(new Contact().name("Desafio Tecnico Claro")));
    }
}
