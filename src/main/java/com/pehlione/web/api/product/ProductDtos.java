package com.pehlione.web.api.product;

import java.math.BigDecimal;
import java.time.Instant;

import com.pehlione.web.product.Product;
import com.pehlione.web.product.ProductStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProductDtos {

    public record CreateProductRequest(
            @NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 10000) String description,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Min(0) int stockQuantity,
            @NotNull ProductStatus status) {
    }

    public record UpdateProductRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 10000) String description,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            @Min(0) int stockQuantity,
            @NotNull ProductStatus status) {
    }

    public record ProductResponse(
            Long id,
            String sku,
            String name,
            String description,
            BigDecimal price,
            String currency,
            int stockQuantity,
            ProductStatus status,
            Instant createdAt,
            Instant updatedAt) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(
                    p.getId(),
                    p.getSku(),
                    p.getName(),
                    p.getDescription(),
                    p.getPrice(),
                    p.getCurrency(),
                    p.getStockQuantity(),
                    p.getStatus(),
                    p.getCreatedAt(),
                    p.getUpdatedAt());
        }
    }
}
