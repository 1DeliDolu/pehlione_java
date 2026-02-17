package com.pehlione.web.cart;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.product.Product;
import com.pehlione.web.product.ProductRepository;
import com.pehlione.web.product.ProductStatus;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@Service
public class CartService {

	private final CartItemRepository cartRepo;
	private final UserRepository userRepo;
	private final ProductRepository productRepo;

	public CartService(CartItemRepository cartRepo, UserRepository userRepo, ProductRepository productRepo) {
		this.cartRepo = cartRepo;
		this.userRepo = userRepo;
		this.productRepo = productRepo;
	}

	@Transactional
	public void upsertItem(String userEmail, Long productId, int qty) {
		if (qty <= 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "quantity must be > 0");
		}

		User user = userRepo.findByEmail(userEmail)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));

		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

		if (product.getStatus() != ProductStatus.ACTIVE) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Product is not active");
		}

		CartItem item = cartRepo.findByUserIdAndProductId(user.getId(), product.getId()).orElseGet(() -> {
			CartItem created = new CartItem();
			created.setUser(user);
			created.setProduct(product);
			return created;
		});

		item.setQuantity(qty);
		cartRepo.save(item);
	}

	@Transactional(readOnly = true)
	public List<CartItem> list(String userEmail) {
		User user = userRepo.findByEmail(userEmail)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
		return cartRepo.findWithProductAndImagesByUserId(user.getId());
	}

	@Transactional
	public void remove(String userEmail, Long productId) {
		User user = userRepo.findByEmail(userEmail)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
		cartRepo.deleteByUserIdAndProductId(user.getId(), productId);
	}

	@Transactional
	public void clear(String userEmail) {
		User user = userRepo.findByEmail(userEmail)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
		cartRepo.deleteAllByUserId(user.getId());
	}
}
