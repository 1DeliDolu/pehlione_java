package com.pehlione.web.api.product;

import java.time.Instant;
import java.util.List;

import com.pehlione.web.product.ProductImage;

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

	public record ImagePageItem(
			Long id,
			String url,
			String altText,
			int sortOrder,
			boolean primary,
			Instant createdAt) {
		public static ImagePageItem from(ProductImage image) {
			return new ImagePageItem(
					image.getId(),
					image.getUrl(),
					image.getAltText(),
					image.getSortOrder(),
					image.isPrimary(),
					image.getCreatedAt());
		}
	}
}
