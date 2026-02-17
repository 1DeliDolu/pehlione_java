package com.pehlione.web.api.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.inventory.InventoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/admin/inventory")
public class AdminInventoryController {

	private final InventoryService service;

	public AdminInventoryController(InventoryService service) {
		this.service = service;
	}

	@PostMapping("/products/{productId}/restock")
	public StockResponse restock(
			Authentication authentication,
			@PathVariable("productId") Long productId,
			@Valid @RequestBody RestockRequest req) {
		int stock = service.restock(authentication.getName(), productId, req.quantity(), req.reason().trim());
		return new StockResponse(stock);
	}

	@PostMapping("/products/{productId}/adjust")
	public StockResponse adjust(
			Authentication authentication,
			@PathVariable("productId") Long productId,
			@Valid @RequestBody AdjustRequest req) {
		int stock = service.adjust(authentication.getName(), productId, req.delta(), req.reason().trim());
		return new StockResponse(stock);
	}

	public record RestockRequest(@Min(1) int quantity, @NotBlank @Size(max = 255) String reason) {
	}

	public record AdjustRequest(@NotNull Integer delta, @NotBlank @Size(max = 255) String reason) {
	}

	public record StockResponse(int stockQuantity) {
	}
}
