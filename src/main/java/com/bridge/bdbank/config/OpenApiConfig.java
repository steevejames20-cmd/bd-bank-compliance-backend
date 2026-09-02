package com.bridge.bdbank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI/Swagger pour la documentation de l'API.
 * Génère automatiquement la documentation disponible à /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure les informations générales de l'API et la sécurité Bearer.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Bridge bd-bank-compliance API")
                .description("API REST pour l'outil de vérification de conformité des données bancaires")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Bridge Team")
                    .email("contact@bridge.com"))
                .license(new License()
                    .name("Propriétaire")
                    .url("https://bridge.com")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Serveur de développement")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Authentification via token Bearer")));
    }
}