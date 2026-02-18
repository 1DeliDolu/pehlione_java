package com.pehlione.web.notification;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.pehlione.web.mail.MailService;

@Service
public class OrderNotificationService {

	private static final DateTimeFormatter MAIL_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
			.withZone(ZoneId.of("Europe/Berlin"));

	private final MailService mailService;

	public OrderNotificationService(MailService mailService) {
		this.mailService = mailService;
	}

	public void sendOrderPaidEmail(OrderPaidEvent event) {
		if (event == null || isBlank(event.userEmail())) {
			return;
		}

		String subject = "Order confirmation - " + safe(event.orderId(), "-");
		String amountText = formatAmount(event.totalAmount()) + " " + safe(event.currency(), "EUR");
		String paidAtText = event.paidAt() == null ? "-" : MAIL_TS_FORMAT.format(event.paidAt());

		String html = """
				<p>Hello,</p>
				<p>Your order has been successfully paid.</p>
				<ul>
				  <li><strong>Order ID:</strong> %s</li>
				  <li><strong>Payment ID:</strong> %s</li>
				  <li><strong>Total:</strong> %s</li>
				  <li><strong>Paid at:</strong> %s</li>
				</ul>
				<p>You can review your order details in your profile.</p>
				<p>Thank you for shopping with pehlione.com.</p>
				"""
				.formatted(
						escapeHtml(safe(event.orderId(), "-")),
						escapeHtml(safe(event.paymentId(), "-")),
						escapeHtml(amountText),
						escapeHtml(paidAtText));

		mailService.sendHtml(event.userEmail(), subject, html);
	}

	public void sendPaymentFailedEmail(PaymentFailedEvent event) {
		if (event == null || isBlank(event.userEmail())) {
			return;
		}

		String subject = "Payment failed - " + safe(event.orderId(), "-");
		String failedAtText = event.failedAt() == null ? "-" : MAIL_TS_FORMAT.format(event.failedAt());
		String html = """
				<p>Hello,</p>
				<p>Payment could not be completed for your order.</p>
				<ul>
				  <li><strong>Order ID:</strong> %s</li>
				  <li><strong>Payment ID:</strong> %s</li>
				  <li><strong>Reason:</strong> %s</li>
				  <li><strong>Failed at:</strong> %s</li>
				</ul>
				<p>Please retry payment from your orders page.</p>
				"""
				.formatted(
						escapeHtml(safe(event.orderId(), "-")),
						escapeHtml(safe(event.paymentId(), "-")),
						escapeHtml(safe(event.reason(), "Unknown")),
						escapeHtml(failedAtText));

		mailService.sendHtml(event.userEmail(), subject, html);
	}

	public void sendRefundRequestedEmail(RefundRequestedEvent event) {
		if (event == null || isBlank(event.userEmail())) {
			return;
		}

		String subject = "Refund request received - " + safe(event.orderId(), "-");
		String requestedAtText = event.requestedAt() == null ? "-" : MAIL_TS_FORMAT.format(event.requestedAt());
		String amountText = formatAmount(event.amount()) + " " + safe(event.currency(), "EUR");
		String html = """
				<p>Hello,</p>
				<p>Your refund request has been received.</p>
				<ul>
				  <li><strong>Order ID:</strong> %s</li>
				  <li><strong>Refund ID:</strong> %s</li>
				  <li><strong>Amount:</strong> %s</li>
				  <li><strong>Reason:</strong> %s</li>
				  <li><strong>Requested at:</strong> %s</li>
				</ul>
				<p>We will notify you once the refund is completed.</p>
				"""
				.formatted(
						escapeHtml(safe(event.orderId(), "-")),
						escapeHtml(safe(event.refundId(), "-")),
						escapeHtml(amountText),
						escapeHtml(safe(event.reason(), "-")),
						escapeHtml(requestedAtText));

		mailService.sendHtml(event.userEmail(), subject, html);
	}

	public void sendRefundSucceededEmail(RefundSucceededEvent event) {
		if (event == null || isBlank(event.userEmail())) {
			return;
		}

		String subject = "Refund completed - " + safe(event.orderId(), "-");
		String succeededAtText = event.succeededAt() == null ? "-" : MAIL_TS_FORMAT.format(event.succeededAt());
		String amountText = formatAmount(event.amount()) + " " + safe(event.currency(), "EUR");
		String html = """
				<p>Hello,</p>
				<p>Your refund has been completed successfully.</p>
				<ul>
				  <li><strong>Order ID:</strong> %s</li>
				  <li><strong>Refund ID:</strong> %s</li>
				  <li><strong>Amount:</strong> %s</li>
				  <li><strong>Completed at:</strong> %s</li>
				</ul>
				"""
				.formatted(
						escapeHtml(safe(event.orderId(), "-")),
						escapeHtml(safe(event.refundId(), "-")),
						escapeHtml(amountText),
						escapeHtml(succeededAtText));

		mailService.sendHtml(event.userEmail(), subject, html);
	}

	public void sendOrderShippedEmail(OrderShippedEvent event) {
		if (event == null || isBlank(event.userEmail())) {
			return;
		}

		String subject = "Order shipped - " + safe(event.orderId(), "-");
		String shippedAtText = event.shippedAt() == null ? "-" : MAIL_TS_FORMAT.format(event.shippedAt());
		String html = """
				<p>Hello,</p>
				<p>Your order has been shipped.</p>
				<ul>
				  <li><strong>Order ID:</strong> %s</li>
				  <li><strong>Shipment ID:</strong> %s</li>
				  <li><strong>Carrier:</strong> %s</li>
				  <li><strong>Tracking Number:</strong> %s</li>
				  <li><strong>Shipped at:</strong> %s</li>
				</ul>
				"""
				.formatted(
						escapeHtml(safe(event.orderId(), "-")),
						escapeHtml(safe(event.shipmentId(), "-")),
						escapeHtml(safe(event.carrier(), "-")),
						escapeHtml(safe(event.trackingNumber(), "-")),
						escapeHtml(shippedAtText));

		mailService.sendHtml(event.userEmail(), subject, html);
	}

	public void sendOrderDeliveredEmail(OrderDeliveredEvent event) {
		if (event == null || isBlank(event.userEmail())) {
			return;
		}

		String subject = "Order delivered - " + safe(event.orderId(), "-");
		String deliveredAtText = event.deliveredAt() == null ? "-" : MAIL_TS_FORMAT.format(event.deliveredAt());
		String html = """
				<p>Hello,</p>
				<p>Your order has been delivered.</p>
				<ul>
				  <li><strong>Order ID:</strong> %s</li>
				  <li><strong>Shipment ID:</strong> %s</li>
				  <li><strong>Delivered at:</strong> %s</li>
				</ul>
				"""
				.formatted(
						escapeHtml(safe(event.orderId(), "-")),
						escapeHtml(safe(event.shipmentId(), "-")),
						escapeHtml(deliveredAtText));

		mailService.sendHtml(event.userEmail(), subject, html);
	}

	private String formatAmount(BigDecimal value) {
		if (value == null) {
			return "0.00";
		}
		return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
	}

	private String safe(String value, String fallback) {
		return isBlank(value) ? fallback : value.trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
