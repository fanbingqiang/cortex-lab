package com.cortex.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cortex API")
                        .version("1.0.0")
                        .description("Cortex 多智能体协作框架 - API 接口文档")
                        .contact(new Contact().name("Cortex Team")));
    }
}
