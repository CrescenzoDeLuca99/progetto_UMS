package com.intesi.usermanagement.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("User Management API")
                        .description("API per la gestione degli utenti — Intesi Group")
                        .version("1.0"));

        if (!issuerUri.isBlank()) {
            String tokenUrl = issuerUri + "/protocol/openid-connect/token";
            openAPI
                    .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                    .components(new Components()
                            .addSecuritySchemes("oauth2", new SecurityScheme()
                                    .type(SecurityScheme.Type.OAUTH2)
                                    .flows(new OAuthFlows()
                                            .password(new OAuthFlow()
                                                    .tokenUrl(tokenUrl)
                                                    .scopes(new Scopes())))));
        }

        return openAPI;
    }
}
