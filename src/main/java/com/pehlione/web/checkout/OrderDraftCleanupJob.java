package com.pehlione.web.checkout;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderDraftCleanupJob {

	private final CheckoutService checkoutService;

	public OrderDraftCleanupJob(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@Scheduled(fixedDelayString = "PT1M")
	@Transactional
	public void markExpired() {
		checkoutService.expireDrafts();
	}
}
