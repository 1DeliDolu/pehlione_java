package com.pehlione.web.product;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;

@Service
public class ProductImageService {

	private final ProductRepository productRepo;

	public ProductImageService(ProductRepository productRepo) {
		this.productRepo = productRepo;
	}

	@Transactional
	public Product addImage(Long productId, String url, String altText) {
		Product p = productRepo.findWithDetailsById(productId).orElseThrow(() ->
				new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

		ProductImage img = new ProductImage();
		img.setProduct(p);
		img.setUrl(url.trim());
		img.setAltText(altText != null ? altText.trim() : null);

		int nextOrder = p.getImages().stream().mapToInt(ProductImage::getSortOrder).max().orElse(-1) + 1;
		img.setSortOrder(nextOrder);

		if (p.getImages().isEmpty()) {
			img.setPrimary(true);
		}

		p.getImages().add(img);
		return p;
	}

	@Transactional
	public Product reorder(Long productId, List<Long> imageIds, Map<Long, Integer> orderMap, Long primaryId) {
		Product p = productRepo.findWithDetailsById(productId).orElseThrow(() ->
				new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

		Set<Long> existing = new HashSet<>();
		for (ProductImage i : p.getImages()) {
			existing.add(i.getId());
		}
		for (Long id : imageIds) {
			if (!existing.contains(id)) {
				throw new ApiException(
						HttpStatus.BAD_REQUEST,
						ApiErrorCode.VALIDATION_FAILED,
						"Image does not belong to product: " + id);
			}
		}

		for (ProductImage i : p.getImages()) {
			i.setPrimary(primaryId != null && primaryId.equals(i.getId()));
			Integer ord = orderMap.get(i.getId());
			if (ord != null) {
				i.setSortOrder(ord);
			}
		}

		if (primaryId == null && !p.getImages().isEmpty()) {
			p.getImages().get(0).setPrimary(true);
		}

		return p;
	}

	@Transactional
	public void delete(Long productId, Long imageId) {
		Product p = productRepo.findWithDetailsById(productId).orElseThrow(() ->
				new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

		ProductImage target = p.getImages().stream()
				.filter(i -> Objects.equals(i.getId(), imageId))
				.findFirst()
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Image not found"));

		boolean wasPrimary = target.isPrimary();
		p.getImages().remove(target);

		if (wasPrimary && !p.getImages().isEmpty()) {
			p.getImages().get(0).setPrimary(true);
		}
	}
}
