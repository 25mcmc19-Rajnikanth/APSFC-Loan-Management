package com.apsfc.loanmanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger metadata. All protected endpoints require the "bearer-jwt"
 * security scheme; the lock icon in Swagger UI lets you paste a token.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI apsfcOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("APSFC Loan Management System API")
                        .description("REST API for the APSFC Loan Management System. "
                                + "Authenticate via /api/auth/login and send the token as "
                                + "Authorization: Bearer <token>.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT obtained from /api/auth/login or /api/auth/register")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}