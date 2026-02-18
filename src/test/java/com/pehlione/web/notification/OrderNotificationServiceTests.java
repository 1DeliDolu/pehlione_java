package com.pehlione.web.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pehlione.web.mail.MailService;

@ExtendWith(MockitoExtension.class)
class OrderNotificationServiceTests {

	@Mock
	private MailService mailService;

	@InjectMocks
	private OrderNotificationService service;

	@Test
	void sendsOrderPaidMailWithOrderAndPaymentDetails() {
		OrderPaidEvent event = new OrderPaidEvent(
				"user@pehlione.com",
				"ord-123",
				"pay-456",
				new BigDecimal("42.5"),
				"EUR",
				Instant.parse("2026-02-17T12:00:00Z"));

		service.sendOrderPaidEmail(event);

		ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendHtml(eq("user@pehlione.com"), subjectCaptor.capture(), htmlCaptor.capture());

		assertThat(subjectCaptor.getValue()).contains("ord-123");
		assertThat(htmlCaptor.getValue()).contains("ord-123");
		assertThat(htmlCaptor.getValue()).contains("pay-456");
		assertThat(htmlCaptor.getValue()).contains("42.50 EUR");
	}

	@Test
	void doesNotSendMailWhenRecipientMissing() {
		OrderPaidEvent event = new OrderPaidEvent(
				" ",
				"ord-123",
				"pay-456",
				new BigDecimal("42.5"),
				"EUR",
				Instant.now());

		service.sendOrderPaidEmail(event);

		verifyNoInteractions(mailService);
	}

	@Test
	void sendsPaymentFailedMailWithReason() {
		PaymentFailedEvent event = new PaymentFailedEvent(
				"user@pehlione.com",
				"ord-123",
				"pay-456",
				"card declined",
				Instant.parse("2026-02-17T12:30:00Z"));

		service.sendPaymentFailedEmail(event);

		ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendHtml(eq("user@pehlione.com"), subjectCaptor.capture(), htmlCaptor.capture());

		assertThat(subjectCaptor.getValue()).contains("Payment failed");
		assertThat(htmlCaptor.getValue()).contains("card declined");
		assertThat(htmlCaptor.getValue()).contains("pay-456");
	}

	@Test
	void sendsRefundSucceededMail() {
		RefundSucceededEvent event = new RefundSucceededEvent(
				"user@pehlione.com",
				"ord-123",
				"ref-789",
				new BigDecimal("15"),
				"EUR",
				Instant.parse("2026-02-17T13:00:00Z"));

		service.sendRefundSucceededEmail(event);

		ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailService).sendHtml(eq("user@pehlione.com"), subjectCaptor.capture(), htmlCaptor.capture());

		assertThat(subjectCaptor.getValue()).contains("Refund completed");
		assertThat(htmlCaptor.getValue()).contains("ref-789");
		assertThat(htmlCaptor.getValue()).contains("15.00 EUR");
	}

	@Test
	void sendsShippedAndDeliveredMails() {
		OrderShippedEvent shipped = new OrderShippedEvent(
				"user@pehlione.com",
				"ord-123",
				"shp-111",
				"DHL",
				"TRK-001",
				Instant.parse("2026-02-17T14:00:00Z"));
		OrderDeliveredEvent delivered = new OrderDeliveredEvent(
				"user@pehlione.com",
				"ord-123",
				"shp-111",
				Instant.parse("2026-02-17T16:00:00Z"));

		service.sendOrderShippedEmail(shipped);
		service.sendOrderDeliveredEmail(delivered);

		ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailService, org.mockito.Mockito.times(2))
				.sendHtml(eq("user@pehlione.com"), subjectCaptor.capture(), htmlCaptor.capture());

		assertThat(subjectCaptor.getAllValues().get(0)).contains("Order shipped");
		assertThat(htmlCaptor.getAllValues().get(0)).contains("DHL");
		assertThat(htmlCaptor.getAllValues().get(0)).contains("TRK-001");
		assertThat(subjectCaptor.getAllValues().get(1)).contains("Order delivered");
		assertThat(htmlCaptor.getAllValues().get(1)).contains("shp-111");
	}
}
