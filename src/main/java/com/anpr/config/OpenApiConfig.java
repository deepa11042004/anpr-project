package com.anpr.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI anprOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ANPR Access Control API")
                        .version("v1")
                        .description("API documentation for ANPR-Based Vehicle Entry Access Control System"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public OpenApiCustomizer publicEndpointsCustomizer() {
        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null) {
                return;
            }
            clearSecurity(paths, "/api/auth/login", PathItem.HttpMethod.POST);
            clearSecurity(paths, "/api/anpr/event", PathItem.HttpMethod.POST);
            clearSecurity(paths, "/api/anpr/health", PathItem.HttpMethod.GET);
        };
    }

    private void clearSecurity(Paths paths, String path, PathItem.HttpMethod method) {
        PathItem pathItem = paths.get(path);
        if (pathItem == null) {
            return;
        }

        Operation operation = switch (method) {
            case GET -> pathItem.getGet();
            case POST -> pathItem.getPost();
            case PUT -> pathItem.getPut();
            case DELETE -> pathItem.getDelete();
            case PATCH -> pathItem.getPatch();
            case HEAD -> pathItem.getHead();
            case OPTIONS -> pathItem.getOptions();
            case TRACE -> pathItem.getTrace();
        };

        if (operation != null) {
            operation.setSecurity(List.of());
        }
    }
}
