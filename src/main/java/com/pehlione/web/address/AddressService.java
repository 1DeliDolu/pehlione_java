package com.pehlione.web.address;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@Service
public class AddressService {

	private final UserRepository userRepo;
	private final UserAddressRepository repo;

	public AddressService(UserRepository userRepo, UserAddressRepository repo) {
		this.userRepo = userRepo;
		this.repo = repo;
	}

	@Transactional(readOnly = true)
	public List<UserAddress> list(String userEmail) {
		User user = findUserOrUnauthorized(userEmail);
		return repo.findByUserIdOrderByIsDefaultDescUpdatedAtDesc(user.getId());
	}

	@Transactional
	public UserAddress create(String userEmail, UserAddress address, boolean makeDefault) {
		User user = findUserOrUnauthorized(userEmail);
		address.setUser(user);

		List<UserAddress> current = repo.findByUserIdOrderByIsDefaultDescUpdatedAtDesc(user.getId());
		if (makeDefault) {
			repo.clearDefault(user.getId());
			address.setDefault(true);
		} else if (current.isEmpty()) {
			address.setDefault(true);
		}

		return repo.save(address);
	}

	@Transactional
	public UserAddress update(String userEmail, Long id, Consumer<UserAddress> updater) {
		User user = findUserOrUnauthorized(userEmail);
		UserAddress address = repo.findByIdAndUserId(id, user.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Address not found"));
		updater.accept(address);
		return address;
	}

	@Transactional
	public void delete(String userEmail, Long id) {
		User user = findUserOrUnauthorized(userEmail);
		UserAddress address = repo.findByIdAndUserId(id, user.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Address not found"));

		boolean wasDefault = address.isDefault();
		repo.delete(address);

		if (wasDefault) {
			List<UserAddress> remaining = repo.findByUserIdOrderByIsDefaultDescUpdatedAtDesc(user.getId());
			if (!remaining.isEmpty()) {
				repo.clearDefault(user.getId());
				remaining.get(0).setDefault(true);
			}
		}
	}

	@Transactional
	public void setDefault(String userEmail, Long id) {
		User user = findUserOrUnauthorized(userEmail);
		UserAddress address = repo.findByIdAndUserId(id, user.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Address not found"));
		repo.clearDefault(user.getId());
		address.setDefault(true);
	}

	@Transactional(readOnly = true)
	public UserAddress getForUserOrThrow(String userEmail, Long id) {
		User user = findUserOrUnauthorized(userEmail);
		return repo.findByIdAndUserId(id, user.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Address not found"));
	}

	private User findUserOrUnauthorized(String email) {
		return userRepo.findByEmail(email)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
	}
}
