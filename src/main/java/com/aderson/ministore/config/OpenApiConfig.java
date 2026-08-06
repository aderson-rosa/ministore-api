package com.aderson.ministore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ministoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MiniStore API")
                        .description("API REST de e-commerce simplificado: catalogo de produtos, "
                                + "carrinho e pedidos com baixa de estoque.")
                        .version("0.1.0")
                        .license(new License().name("MIT")));
    }
}
