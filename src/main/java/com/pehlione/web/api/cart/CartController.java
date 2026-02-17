package com.pehlione.web.api.cart;

import static com.pehlione.web.api.cart.CartDtos.CartItemResponse;
import static com.pehlione.web.api.cart.CartDtos.CartResponse;
import static com.pehlione.web.api.cart.CartDtos.UpsertCartItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.cart.CartService;
import com.pehlione.web.product.ProductImage;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Cart", description = "Shopping cart endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping
	public CartResponse get(@AuthenticationPrincipal Jwt jwt) {
		var items = cartService.list(jwt.getSubject()).stream().map(ci -> {
			var p = ci.getProduct();
			String primaryUrl = p.getImages().stream()
					.filter(ProductImage::isPrimary)
					.findFirst()
					.map(ProductImage::getUrl)
					.orElse(null);

			BigDecimal unit = p.getPrice();
			BigDecimal line = unit.multiply(BigDecimal.valueOf(ci.getQuantity()))
					.setScale(2, RoundingMode.HALF_UP);

			return new CartItemResponse(
					p.getId(),
					p.getSku(),
					p.getName(),
					unit,
					p.getCurrency(),
					ci.getQuantity(),
					line,
					primaryUrl);
		}).toList();

		return new CartResponse(items);
	}

	@PostMapping("/items")
	public void upsert(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpsertCartItemRequest req) {
		cartService.upsertItem(jwt.getSubject(), req.productId(), req.quantity());
	}

	@DeleteMapping("/items/{productId}")
	public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable("productId") Long productId) {
		cartService.remove(jwt.getSubject(), productId);
	}

	@DeleteMapping
	public void clear(@AuthenticationPrincipal Jwt jwt) {
		cartService.clear(jwt.getSubject());
	}
}
