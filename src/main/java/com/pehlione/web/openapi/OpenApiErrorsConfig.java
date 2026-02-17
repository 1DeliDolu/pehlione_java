package com.pehlione.web.openapi;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Configuration
public class OpenApiErrorsConfig {

	@Bean
	GlobalOpenApiCustomizer apiProblemSchemaCustomizer() {
		return openApi -> {
			Components components = ensureComponents(openApi);

			Schema<?> violation = new ObjectSchema()
					.addProperty("field", new StringSchema())
					.addProperty("path", new StringSchema())
					.addProperty("message", new StringSchema())
					.addProperty("rejectedValue", new ObjectSchema())
					.addProperty("invalidValue", new ObjectSchema());

			Schema<?> apiProblem = new ObjectSchema()
					.addProperty("type", new StringSchema().example("urn:problem:validation"))
					.addProperty("title", new StringSchema().example("Validation error"))
					.addProperty("status", new IntegerSchema().example(400))
					.addProperty("detail", new StringSchema().example("Validation failed"))
					.addProperty("instance", new StringSchema().example("/api/v1/products"))
					.addProperty("code", new StringSchema().example("VALIDATION_FAILED"))
					.addProperty("requestId", new StringSchema().example("3f9d..."))
					.addProperty("timestamp", new StringSchema().example("2026-02-17T12:34:56Z"))
					.addProperty("retryAfterSeconds", new IntegerSchema().example(12))
					.addProperty("violations", new ArraySchema().items(violation));

			components.addSchemas("ApiProblem", apiProblem);
		};
	}

	@Bean
	GlobalOperationCustomizer globalProblemResponsesCustomizer() {
		return (Operation operation, HandlerMethod handlerMethod) -> {
			ApiResponses responses = operation.getResponses();
			if (responses == null) {
				responses = new ApiResponses();
				operation.setResponses(responses);
			}

			addProblem(responses, "400", "Bad Request / Validation");
			addProblem(responses, "401", "Unauthorized");
			addProblem(responses, "403", "Forbidden");
			addProblem(responses, "409", "Conflict");
			addProblem(responses, "429", "Too Many Requests");
			addProblem(responses, "500", "Internal Server Error");
			return operation;
		};
	}

	private void addProblem(ApiResponses responses, String statusCode, String description) {
		if (responses.containsKey(statusCode)) {
			return;
		}

		Content content = new Content().addMediaType(
				"application/problem+json",
				new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiProblem")));

		ApiResponse response = new ApiResponse()
				.description(description)
				.content(content);
		responses.addApiResponse(statusCode, response);
	}

	private Components ensureComponents(OpenAPI openApi) {
		Components components = openApi.getComponents();
		if (components == null) {
			components = new Components();
			openApi.setComponents(components);
		}
		return components;
	}
}
