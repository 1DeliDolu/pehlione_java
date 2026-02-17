package com.pehlione.web.api.product;

import static com.pehlione.web.api.product.ProductDtos.ProductResponse;
import static com.pehlione.web.api.product.ProductImageDtos.AddImageRequest;
import static com.pehlione.web.api.product.ProductImageDtos.ReorderItem;
import static com.pehlione.web.api.product.ProductImageDtos.ReorderRequest;

import java.util.HashMap;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.product.Product;
import com.pehlione.web.product.ProductImageService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Product Images", description = "Product image management endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/products/{productId}/images")
public class ProductImageController {

	private final ProductImageService service;

	public ProductImageController(ProductImageService service) {
		this.service = service;
	}

	@PostMapping
	public ProductResponse add(@PathVariable("productId") Long productId, @Valid @RequestBody AddImageRequest req) {
		Product p = service.addImage(productId, req.url(), req.altText());
		return ProductResponse.from(p);
	}

	@PutMapping("/reorder")
	public ProductResponse reorder(@PathVariable("productId") Long productId, @Valid @RequestBody ReorderRequest req) {
		var orderMap = new HashMap<Long, Integer>();
		Long primaryId = null;

		for (ReorderItem it : req.items()) {
			orderMap.put(it.id(), it.sortOrder());
			if (it.primary()) {
				primaryId = it.id();
			}
		}

		Product p = service.reorder(
				productId,
				req.items().stream().map(ReorderItem::id).toList(),
				orderMap,
				primaryId);
		return ProductResponse.from(p);
	}

	@DeleteMapping("/{imageId}")
	public void delete(@PathVariable("productId") Long productId, @PathVariable("imageId") Long imageId) {
		service.delete(productId, imageId);
	}
}
