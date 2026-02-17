package com.pehlione.web.api.product;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductImageDtos {

	public record AddImageRequest(
			@NotBlank @Size(max = 1000) String url,
			@Size(max = 255) String altText) {
	}

	public record ReorderItem(
			@NotNull Long id,
			int sortOrder,
			boolean primary) {
	}

	public record ReorderRequest(
			@NotEmpty List<@NotNull ReorderItem> items) {
	}
}
