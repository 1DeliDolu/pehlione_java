package com.pehlione.web.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderNotificationListener {

	private final OrderNotificationService notificationService;

	public OrderNotificationListener(OrderNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderPaid(OrderPaidEvent event) {
		notificationService.sendOrderPaidEmail(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPaymentFailed(PaymentFailedEvent event) {
		notificationService.sendPaymentFailedEmail(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onRefundRequested(RefundRequestedEvent event) {
		notificationService.sendRefundRequestedEmail(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onRefundSucceeded(RefundSucceededEvent event) {
		notificationService.sendRefundSucceededEmail(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderShipped(OrderShippedEvent event) {
		notificationService.sendOrderShippedEmail(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderDelivered(OrderDeliveredEvent event) {
		notificationService.sendOrderDeliveredEmail(event);
	}
}
