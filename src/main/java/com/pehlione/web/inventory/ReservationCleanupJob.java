package com.pehlione.web.inventory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationCleanupJob {

	private final InventoryService inventoryService;

	public ReservationCleanupJob(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@Scheduled(fixedDelayString = "PT1M")
	public void releaseExpired() {
		inventoryService.releaseExpiredReservations();
	}
}
