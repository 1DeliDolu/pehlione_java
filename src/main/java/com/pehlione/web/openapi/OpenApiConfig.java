package com.pehlione.web.openapi;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI baseOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Pehlione Web API")
						.version("v1")
						.description("E-commerce backend API (Spring Boot 4)"))
				.components(new Components()
						.addSecuritySchemes("bearerAuth",
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
	}

	@Bean
	GroupedOpenApi publicApi() {
		return GroupedOpenApi.builder()
				.group("public")
				.pathsToMatch("/api/v1/**")
				.pathsToExclude("/api/v1/admin/**", "/api/v1/webhooks/**")
				.build();
	}

	@Bean
	GroupedOpenApi adminApi() {
		return GroupedOpenApi.builder()
				.group("admin")
				.pathsToMatch("/api/v1/admin/**")
				.build();
	}

	@Bean
	GroupedOpenApi webhooksApi() {
		return GroupedOpenApi.builder()
				.group("webhooks")
				.pathsToMatch("/api/v1/webhooks/**")
				.build();
	}

	@Bean
	OpenApiCustomizer openEndpointsWithoutAuthCustomizer() {
		return openApi -> {
			if (openApi.getPaths() == null) {
				return;
			}

			openApi.getPaths().forEach((path, pathItem) -> {
				if (!isPublicPath(path)) {
					return;
				}
				clearSecurity(pathItem);
			});
		};
	}

	private boolean isPublicPath(String path) {
		return path != null && (path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/webhooks/"));
	}

	private void clearSecurity(PathItem pathItem) {
		if (pathItem == null) {
			return;
		}
		if (pathItem.getGet() != null) {
			pathItem.getGet().setSecurity(List.of());
		}
		if (pathItem.getPost() != null) {
			pathItem.getPost().setSecurity(List.of());
		}
		if (pathItem.getPut() != null) {
			pathItem.getPut().setSecurity(List.of());
		}
		if (pathItem.getPatch() != null) {
			pathItem.getPatch().setSecurity(List.of());
		}
		if (pathItem.getDelete() != null) {
			pathItem.getDelete().setSecurity(List.of());
		}
		if (pathItem.getHead() != null) {
			pathItem.getHead().setSecurity(List.of());
		}
		if (pathItem.getOptions() != null) {
			pathItem.getOptions().setSecurity(List.of());
		}
		if (pathItem.getTrace() != null) {
			pathItem.getTrace().setSecurity(List.of());
		}
	}
}
