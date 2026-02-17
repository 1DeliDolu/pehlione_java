package com.pehlione.web.inventory;

import java.time.Instant;

import com.pehlione.web.product.Product;
import com.pehlione.web.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_events")
public class InventoryEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id")
	private InventoryReservation reservation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_user_id")
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private InventoryEventType type;

	@Column(name = "quantity_delta", nullable = false)
	private int quantityDelta;

	@Column(length = 255)
	private String reason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public InventoryReservation getReservation() {
		return reservation;
	}

	public void setReservation(InventoryReservation reservation) {
		this.reservation = reservation;
	}

	public User getActor() {
		return actor;
	}

	public void setActor(User actor) {
		this.actor = actor;
	}

	public InventoryEventType getType() {
		return type;
	}

	public void setType(InventoryEventType type) {
		this.type = type;
	}

	public int getQuantityDelta() {
		return quantityDelta;
	}

	public void setQuantityDelta(int quantityDelta) {
		this.quantityDelta = quantityDelta;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
