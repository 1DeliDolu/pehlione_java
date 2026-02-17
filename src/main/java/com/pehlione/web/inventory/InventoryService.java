package com.pehlione.web.inventory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.product.Product;
import com.pehlione.web.product.ProductRepository;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@Service
public class InventoryService {

	private final ProductRepository productRepo;
	private final UserRepository userRepo;
	private final InventoryReservationRepository reservationRepo;
	private final InventoryEventRepository eventRepo;

	public InventoryService(
			ProductRepository productRepo,
			UserRepository userRepo,
			InventoryReservationRepository reservationRepo,
			InventoryEventRepository eventRepo) {
		this.productRepo = productRepo;
		this.userRepo = userRepo;
		this.reservationRepo = reservationRepo;
		this.eventRepo = eventRepo;
	}

	public record ReservationResult(String reservationId, Instant expiresAt, int remainingStock) {
	}

	@Transactional
	public ReservationResult reserve(String userEmail, Long productId, int qty, Integer ttlMinutes) {
		if (qty <= 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "qty must be > 0");
		}

		int effectiveTtl = ttlMinutes == null ? 15 : ttlMinutes;
		if (effectiveTtl <= 0 || effectiveTtl > 120) {
			effectiveTtl = 15;
		}

		User user = findUserByEmailOrUnauthorized(userEmail);
		Product product = productRepo.findForUpdate(productId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

		if (product.getStockQuantity() < qty) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Insufficient stock");
		}

		product.setStockQuantity(product.getStockQuantity() - qty);

		InventoryReservation reservation = new InventoryReservation();
		reservation.setPublicId(UUID.randomUUID().toString());
		reservation.setProduct(product);
		reservation.setUser(user);
		reservation.setQuantity(qty);
		reservation.setStatus(ReservationStatus.ACTIVE);
		reservation.setExpiresAt(Instant.now().plus(effectiveTtl, ChronoUnit.MINUTES));
		reservationRepo.save(reservation);

		writeEvent(product, reservation, user, InventoryEventType.RESERVE, -qty, "reserve");

		return new ReservationResult(reservation.getPublicId(), reservation.getExpiresAt(), product.getStockQuantity());
	}

	@Transactional
	public void release(String userEmail, String reservationId) {
		User user = findUserByEmailOrUnauthorized(userEmail);

		InventoryReservation reservation = reservationRepo.findByPublicIdForUpdate(reservationId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Reservation not found"));

		if (!reservation.getUser().getId().equals(user.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN, "Not your reservation");
		}

		if (reservation.getStatus() != ReservationStatus.ACTIVE) {
			return;
		}

		Product product = productRepo.findForUpdate(reservation.getProduct().getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

		reservation.setStatus(ReservationStatus.RELEASED);
		product.setStockQuantity(product.getStockQuantity() + reservation.getQuantity());
		writeEvent(product, reservation, user, InventoryEventType.RELEASE, reservation.getQuantity(), "release");
	}

	@Transactional(noRollbackFor = ApiException.class)
	public void consume(String userEmail, String reservationId) {
		User user = findUserByEmailOrUnauthorized(userEmail);

		InventoryReservation reservation = reservationRepo.findByPublicIdForUpdate(reservationId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Reservation not found"));

		if (!reservation.getUser().getId().equals(user.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN, "Not your reservation");
		}

		if (reservation.getStatus() != ReservationStatus.ACTIVE) {
			return;
		}

		if (reservation.getExpiresAt().isBefore(Instant.now())) {
			Product product = productRepo.findForUpdate(reservation.getProduct().getId())
					.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

			reservation.setStatus(ReservationStatus.EXPIRED);
			product.setStockQuantity(product.getStockQuantity() + reservation.getQuantity());
			writeEvent(product, reservation, user, InventoryEventType.RELEASE, reservation.getQuantity(),
					"auto-expired on consume");
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Reservation expired");
		}

		reservation.setStatus(ReservationStatus.CONSUMED);
		writeEvent(reservation.getProduct(), reservation, user, InventoryEventType.CONSUME, 0, "consume");
	}

	@Transactional
	public void systemRelease(String reservationId, String reason) {
		InventoryReservation reservation = reservationRepo.findByPublicIdForUpdate(reservationId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Reservation not found"));

		if (reservation.getStatus() != ReservationStatus.ACTIVE) {
			return;
		}

		Product product = productRepo.findForUpdate(reservation.getProduct().getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

		reservation.setStatus(ReservationStatus.RELEASED);
		product.setStockQuantity(product.getStockQuantity() + reservation.getQuantity());
		writeEvent(
				product,
				reservation,
				null,
				InventoryEventType.RELEASE,
				reservation.getQuantity(),
				reason == null ? "system-release" : reason);
	}

	@Transactional(noRollbackFor = ApiException.class)
	public void systemConsume(String reservationId, String reason) {
		InventoryReservation reservation = reservationRepo.findByPublicIdForUpdate(reservationId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Reservation not found"));

		if (reservation.getStatus() != ReservationStatus.ACTIVE) {
			return;
		}

		if (reservation.getExpiresAt().isBefore(Instant.now())) {
			Product product = productRepo.findForUpdate(reservation.getProduct().getId())
					.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));
			reservation.setStatus(ReservationStatus.EXPIRED);
			product.setStockQuantity(product.getStockQuantity() + reservation.getQuantity());
			writeEvent(
					product,
					reservation,
					null,
					InventoryEventType.RELEASE,
					reservation.getQuantity(),
					"system-expired-on-consume");
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Reservation expired");
		}

		reservation.setStatus(ReservationStatus.CONSUMED);
		writeEvent(
				reservation.getProduct(),
				reservation,
				null,
				InventoryEventType.CONSUME,
				0,
				reason == null ? "system-consume" : reason);
	}

	@Transactional
	public int restock(String actorEmail, Long productId, int qty, String reason) {
		if (qty <= 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "qty must be > 0");
		}

		User actor = actorEmail == null ? null : userRepo.findByEmail(actorEmail).orElse(null);
		Product product = productRepo.findForUpdate(productId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

		product.setStockQuantity(product.getStockQuantity() + qty);
		writeEvent(product, null, actor, InventoryEventType.RESTOCK, qty, reason);
		return product.getStockQuantity();
	}

	@Transactional
	public int adjust(String actorEmail, Long productId, int delta, String reason) {
		if (delta == 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "delta must be != 0");
		}

		User actor = actorEmail == null ? null : userRepo.findByEmail(actorEmail).orElse(null);
		Product product = productRepo.findForUpdate(productId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));

		int next = product.getStockQuantity() + delta;
		if (next < 0) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Stock cannot go below zero");
		}

		product.setStockQuantity(next);
		writeEvent(product, null, actor, InventoryEventType.ADJUST, delta, reason);
		return product.getStockQuantity();
	}

	@Transactional
	public int releaseExpiredReservations() {
		List<InventoryReservation> expired = reservationRepo.lockByStatusAndExpiresBefore(ReservationStatus.ACTIVE, Instant.now());
		int released = 0;
		for (InventoryReservation reservation : expired) {
			if (reservation.getStatus() != ReservationStatus.ACTIVE) {
				continue;
			}

			Product product = productRepo.findForUpdate(reservation.getProduct().getId()).orElse(null);
			if (product == null) {
				continue;
			}

			reservation.setStatus(ReservationStatus.EXPIRED);
			product.setStockQuantity(product.getStockQuantity() + reservation.getQuantity());
			writeEvent(product, reservation, null, InventoryEventType.RELEASE, reservation.getQuantity(), "auto-expire");
			released++;
		}
		return released;
	}

	private User findUserByEmailOrUnauthorized(String email) {
		return userRepo.findByEmail(email)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
	}

	private void writeEvent(
			Product product,
			InventoryReservation reservation,
			User actor,
			InventoryEventType type,
			int delta,
			String reason) {
		InventoryEvent event = new InventoryEvent();
		event.setProduct(product);
		event.setReservation(reservation);
		event.setActor(actor);
		event.setType(type);
		event.setQuantityDelta(delta);
		event.setReason(reason);
		eventRepo.save(event);
	}
}
