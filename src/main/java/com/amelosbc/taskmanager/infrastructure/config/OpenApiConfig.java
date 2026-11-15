package com.amelosbc.taskmanager.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger Configuration.
 *
 * Configures the interactive Swagger UI to include the Authorize button
 * supporting both Dev Mock Auth (X-Mock-User header) and Clerk Bearer JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String DEV_MOCK_SCHEME = "DevMockUser";
    private static final String BEARER_JWT_SCHEME = "ClerkBearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskManager SaaS Kanban API")
                        .description("API REST com Clean Architecture, Spring Security, Dev Mock Auth e Clerk.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DIO TaskManager Team")
                                .email("dev@taskmanager.com")))
                // Adiciona os esquemas de segurança aos endpoints por padrão
                .addSecurityItem(new SecurityRequirement()
                        .addList(DEV_MOCK_SCHEME)
                        .addList(BEARER_JWT_SCHEME))
                .components(new Components()
                        // 1. Dev Mock Header: X-Mock-User
                        .addSecuritySchemes(DEV_MOCK_SCHEME, new SecurityScheme()
                                .name("X-Mock-User")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Insira qualquer identificador de usuário para ambiente local (ex: asmelo, test_user)"))
                        // 2. Bearer JWT (para tokens reais do Clerk)
                        .addSecuritySchemes(BEARER_JWT_SCHEME, new SecurityScheme()
                                .name(BEARER_JWT_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT emitido pelo Clerk")));
    }
}
