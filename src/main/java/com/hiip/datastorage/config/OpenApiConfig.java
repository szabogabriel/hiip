package com.hiip.datastorage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${hiip.openapi.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${hiip.openapi.prod-url:}")
    private String prodUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        // Define JWT Security Scheme
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // Define Security Requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        // Create Server list
        Server devServer = new Server()
                .url(devUrl)
                .description("Development Server");

        List<Server> servers = new java.util.ArrayList<>();
        servers.add(devServer);

        if (!prodUrl.isEmpty()) {
            Server prodServer = new Server()
                    .url(prodUrl)
                    .description("Production Server");
            servers.add(prodServer);
        }

        return new OpenAPI()
                .info(new Info()
                        .title("HIIP Data Storage API")
                        .description("High-Performance Intelligent Information Platform - A universal data storage solution with JWT authentication")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HIIP Team")
                                .email("support@hiip.example.com")
                                .url("https://github.com/szabogabriel/hiip"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}
