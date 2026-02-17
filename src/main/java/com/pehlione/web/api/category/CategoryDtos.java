package com.pehlione.web.api.category;

import java.time.Instant;

import com.pehlione.web.category.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryDtos {

	public record CreateCategoryRequest(
			@NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max = 64) String slug,
			@NotBlank @Size(max = 255) String name) {
	}

	public record UpdateCategoryRequest(
			@NotBlank @Size(max = 255) String name) {
	}

	public record CategoryResponse(
			Long id,
			String slug,
			String name,
			Instant createdAt,
			Instant updatedAt) {
		public static CategoryResponse from(Category c) {
			return new CategoryResponse(c.getId(), c.getSlug(), c.getName(), c.getCreatedAt(), c.getUpdatedAt());
		}
	}
}
