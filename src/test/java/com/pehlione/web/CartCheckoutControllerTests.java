package com.pehlione.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CartCheckoutControllerTests {

	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern DRAFT_ID_PATTERN = Pattern.compile("\"draftId\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("\"reservationId\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern ORDER_ID_PATTERN = Pattern.compile("\"orderId\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern PAYMENT_ID_PATTERN = Pattern.compile("\"paymentId\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern SHIPMENT_ID_PATTERN = Pattern.compile("\"shipmentId\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern ADDRESS_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
	private static final Pattern REFUND_ID_PATTERN = Pattern.compile("\"refundId\"\\s*:\\s*\"([^\"]+)\"");
	private static final String WEBHOOK_SECRET = "CHANGE_ME_SUPER_SECRET";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
		resetState();
	}

	@Test
	void reserveThenSubmitConsumesReservationAndKeepsStockReduced() throws Exception {
		Long productId = insertProduct("SKU-CHECK-1", "Checkout Tee", 20);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":3}
						""".formatted(productId)))
				.andExpect(status().isOk());

		String cartBody = mockMvc.perform(get("/api/v1/cart")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(cartBody).contains("Checkout Tee");
		assertThat(cartBody).contains("\"quantity\":3");

		String reserveBody = mockMvc.perform(post("/api/v1/checkout/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"ttlMinutes":15}
						"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String draftId = extractField(reserveBody, DRAFT_ID_PATTERN);
		String reservationId = extractField(reserveBody, RESERVATION_ID_PATTERN);

		Integer stockAfterReserve = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		Integer cartCountAfterReserve = jdbcTemplate.queryForObject(
				"select count(*) from cart_items where user_id = (select id from users where email = ?)",
				Integer.class,
				"user@pehlione.com");
		assertThat(stockAfterReserve).isEqualTo(17);
		assertThat(cartCountAfterReserve).isEqualTo(0);

		String submitBody = mockMvc.perform(post("/api/v1/checkout/drafts/{draftId}/submit", draftId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String orderId = extractField(submitBody, ORDER_ID_PATTERN);

		String draftStatus = jdbcTemplate.queryForObject(
				"select status from order_drafts where public_id = ?",
				String.class,
				draftId);
		String reservationStatus = jdbcTemplate.queryForObject(
				"select status from inventory_reservations where public_id = ?",
				String.class,
				reservationId);
		Integer stockAfterSubmit = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		Integer createdOrders = jdbcTemplate.queryForObject(
				"select count(*) from orders where public_id = ? and source_draft_public_id = ?",
				Integer.class,
				orderId,
				draftId);
		Integer createdOrderItems = jdbcTemplate.queryForObject(
				"""
						select count(*)
						from order_items oi
						join orders o on o.id = oi.order_id
						where o.public_id = ?
						""",
				Integer.class,
				orderId);
		assertThat(draftStatus).isEqualTo("SUBMITTED");
		assertThat(reservationStatus).isEqualTo("CONSUMED");
		assertThat(stockAfterSubmit).isEqualTo(17);
		assertThat(createdOrders).isEqualTo(1);
		assertThat(createdOrderItems).isEqualTo(1);

		String listBody = mockMvc.perform(get("/api/v1/orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(listBody).contains(orderId);

		String detailBody = mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(detailBody).contains(orderId);
		assertThat(detailBody).contains("\"status\":\"PLACED\"");
	}

	@Test
	void reserveThenCancelReleasesReservationAndRestoresStock() throws Exception {
		Long productId = insertProduct("SKU-CHECK-2", "Cancel Hoodie", 9);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":4}
						""".formatted(productId)))
				.andExpect(status().isOk());

		String reserveBody = mockMvc.perform(post("/api/v1/checkout/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String draftId = extractField(reserveBody, DRAFT_ID_PATTERN);
		String reservationId = extractField(reserveBody, RESERVATION_ID_PATTERN);

		mockMvc.perform(post("/api/v1/checkout/drafts/{draftId}/cancel", draftId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk());

		String draftStatus = jdbcTemplate.queryForObject(
				"select status from order_drafts where public_id = ?",
				String.class,
				draftId);
		String reservationStatus = jdbcTemplate.queryForObject(
				"select status from inventory_reservations where public_id = ?",
				String.class,
				reservationId);
		Integer stockAfterCancel = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		assertThat(draftStatus).isEqualTo("CANCELLED");
		assertThat(reservationStatus).isEqualTo("RELEASED");
		assertThat(stockAfterCancel).isEqualTo(9);
	}

	@Test
	void reserveThenPayAndConfirmMarksOrderPaidAndConsumesReservation() throws Exception {
		Long productId = insertProduct("SKU-CHECK-3", "Pay Sweatshirt", 12);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":2}
						""".formatted(productId)))
				.andExpect(status().isOk());

		String reserveBody = mockMvc.perform(post("/api/v1/checkout/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String draftId = extractField(reserveBody, DRAFT_ID_PATTERN);
		String reservationId = extractField(reserveBody, RESERVATION_ID_PATTERN);
		Long addressId = createAddressAndReturnId(userAccess, "Home");

		String payBody1 = mockMvc.perform(post("/api/v1/checkout/drafts/{draftId}/pay", draftId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.header("Idempotency-Key", "idem-" + draftId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
							"addressId":%d,
							"cardHolderName":"Max Mustermann",
							"cardNumber":"4242 4242 4242 4242",
							"expiryMonth":12,
							"expiryYear":2032,
							"cvc":"123"
						}
						""".formatted(addressId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String paymentId = extractField(payBody1, PAYMENT_ID_PATTERN);
		String orderId = extractField(payBody1, ORDER_ID_PATTERN);

		String payBody2 = mockMvc.perform(post("/api/v1/checkout/drafts/{draftId}/pay", draftId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.header("Idempotency-Key", "idem-" + draftId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
							"addressId":%d,
							"cardHolderName":"Max Mustermann",
							"cardNumber":"4242 4242 4242 4242",
							"expiryMonth":12,
							"expiryYear":2032,
							"cvc":"123"
						}
						""".formatted(addressId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(extractField(payBody2, PAYMENT_ID_PATTERN)).isEqualTo(paymentId);
		assertThat(extractField(payBody2, ORDER_ID_PATTERN)).isEqualTo(orderId);

		String draftStatusAfterPay = jdbcTemplate.queryForObject(
				"select status from order_drafts where public_id = ?",
				String.class,
				draftId);
		String orderStatusAfterPay = jdbcTemplate.queryForObject(
				"select status from orders where public_id = ?",
				String.class,
				orderId);
		String paymentStatusAfterPay = jdbcTemplate.queryForObject(
				"select status from payment_intents where public_id = ?",
				String.class,
				paymentId);
		assertThat(draftStatusAfterPay).isEqualTo("PAYMENT_PENDING");
		assertThat(orderStatusAfterPay).isEqualTo("PENDING_PAYMENT");
		assertThat(paymentStatusAfterPay).isEqualTo("REQUIRES_CONFIRMATION");

		mockMvc.perform(post("/api/v1/payments/{paymentId}/confirm-mock", paymentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk());

		String paymentBody = mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(paymentBody).contains("\"status\":\"SUCCEEDED\"");
		assertThat(paymentBody).contains(orderId);

		String orderDetailBody = mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(orderDetailBody).contains("\"shippingAddress\":");
		assertThat(orderDetailBody).contains("\"line1\":\"Street 1\"");
		assertThat(orderDetailBody).contains("\"countryCode\":\"DE\"");

		String draftStatus = jdbcTemplate.queryForObject(
				"select status from order_drafts where public_id = ?",
				String.class,
				draftId);
		String reservationStatus = jdbcTemplate.queryForObject(
				"select status from inventory_reservations where public_id = ?",
				String.class,
				reservationId);
		String orderStatus = jdbcTemplate.queryForObject(
				"select status from orders where public_id = ?",
				String.class,
				orderId);
		String paymentStatus = jdbcTemplate.queryForObject(
				"select status from payment_intents where public_id = ?",
				String.class,
				paymentId);
		Integer stockAfterConfirm = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		assertThat(draftStatus).isEqualTo("SUBMITTED");
		assertThat(reservationStatus).isEqualTo("CONSUMED");
		assertThat(orderStatus).isEqualTo("PAID");
		assertThat(paymentStatus).isEqualTo("SUCCEEDED");
		assertThat(stockAfterConfirm).isEqualTo(10);
	}

	@Test
	void payRejectsIncompleteCardPayload() throws Exception {
		Long productId = insertProduct("SKU-CHECK-3B", "Pay Validation Tee", 5);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":1}
						""".formatted(productId)))
				.andExpect(status().isOk());

		String reserveBody = mockMvc.perform(post("/api/v1/checkout/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String draftId = extractField(reserveBody, DRAFT_ID_PATTERN);
		Long addressId = createAddressAndReturnId(userAccess, "Validation Home");

		String responseBody = mockMvc.perform(post("/api/v1/checkout/drafts/{draftId}/pay", draftId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
							"addressId":%d,
							"cardNumber":"4242 4242 4242 4242"
						}
						""".formatted(addressId)))
				.andExpect(status().isBadRequest())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(responseBody).contains("All card fields are required");
	}

	@Test
	void adminCanShipAndDeliverPaidOrder() throws Exception {
		Long productId = insertProduct("SKU-CHECK-4", "Ship Jacket", 7);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");
		String adminAccess = loginAndGetAccessToken("admin@pehlione.com", "admin123");

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":1}
						""".formatted(productId)))
				.andExpect(status().isOk());

		String reserveBody = mockMvc.perform(post("/api/v1/checkout/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String draftId = extractField(reserveBody, DRAFT_ID_PATTERN);
		String reservationId = extractField(reserveBody, RESERVATION_ID_PATTERN);
		Long addressId = createAddressAndReturnId(userAccess, "Office");

		String payBody = mockMvc.perform(post("/api/v1/checkout/drafts/{draftId}/pay", draftId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.header("Idempotency-Key", "idem-ship-" + draftId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"addressId":%d}
						""".formatted(addressId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String paymentId = extractField(payBody, PAYMENT_ID_PATTERN);
		String orderId = extractField(payBody, ORDER_ID_PATTERN);

		mockMvc.perform(post("/api/v1/payments/{paymentId}/confirm-mock", paymentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk());

		String shipBody = mockMvc.perform(post("/api/v1/admin/orders/{orderId}/ship", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"carrier":"DHL","trackingNumber":"TRACK-001"}
						"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String shipmentId = extractField(shipBody, SHIPMENT_ID_PATTERN);

		String orderStatusAfterShip = jdbcTemplate.queryForObject(
				"select status from orders where public_id = ?",
				String.class,
				orderId);
		String shipmentStatusAfterShip = jdbcTemplate.queryForObject(
				"select status from shipments where public_id = ?",
				String.class,
				shipmentId);
		assertThat(orderStatusAfterShip).isEqualTo("SHIPPED");
		assertThat(shipmentStatusAfterShip).isEqualTo("SHIPPED");

		String detailAfterShip = mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(detailAfterShip).contains("\"shipments\":[");
		assertThat(detailAfterShip).contains(shipmentId);
		assertThat(detailAfterShip).contains("\"status\":\"SHIPPED\"");

		mockMvc.perform(post("/api/v1/admin/orders/{orderId}/deliver", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess))
				.andExpect(status().isOk());

		String orderStatusAfterDeliver = jdbcTemplate.queryForObject(
				"select status from orders where public_id = ?",
				String.class,
				orderId);
		String shipmentStatusAfterDeliver = jdbcTemplate.queryForObject(
				"select status from shipments where public_id = ?",
				String.class,
				shipmentId);
		String reservationStatus = jdbcTemplate.queryForObject(
				"select status from inventory_reservations where public_id = ?",
				String.class,
				reservationId);
		assertThat(orderStatusAfterDeliver).isEqualTo("FULFILLED");
		assertThat(shipmentStatusAfterDeliver).isEqualTo("DELIVERED");
		assertThat(reservationStatus).isEqualTo("CONSUMED");
	}

	@Test
	void paymentFailedWebhookReleasesReservationAndIsIdempotent() throws Exception {
		Long productId = insertProduct("SKU-CHECK-5", "Webhook Fail Tee", 6);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":2}
						""".formatted(productId)))
				.andExpect(status().isOk());

		String reserveBody = mockMvc.perform(post("/api/v1/checkout/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String draftId = extractField(reserveBody, DRAFT_ID_PATTERN);
		String reservationId = extractField(reserveBody, RESERVATION_ID_PATTERN);
		Long addressId = createAddressAndReturnId(userAccess, "Webhook Home");

		String payBody = mockMvc.perform(post("/api/v1/checkout/drafts/{draftId}/pay", draftId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.header("Idempotency-Key", "idem-webhook-fail-" + draftId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"addressId":%d}
						""".formatted(addressId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String paymentId = extractField(payBody, PAYMENT_ID_PATTERN);
		String orderId = extractField(payBody, ORDER_ID_PATTERN);

		String eventId = "evt_pay_fail_" + draftId;
		String payload = """
				{"eventId":"%s","type":"payment.failed","paymentId":"%s","orderId":"%s","error":"declined"}
				""".formatted(eventId, paymentId, orderId).trim();
		mockMvc.perform(post("/api/v1/webhooks/mock-payment")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Signature", hmacSha256Hex(payload, WEBHOOK_SECRET))
				.content(payload))
				.andExpect(status().isOk());

		String reservationStatus = jdbcTemplate.queryForObject(
				"select status from inventory_reservations where public_id = ?",
				String.class,
				reservationId);
		String orderStatus = jdbcTemplate.queryForObject(
				"select status from orders where public_id = ?",
				String.class,
				orderId);
		String paymentStatus = jdbcTemplate.queryForObject(
				"select status from payment_intents where public_id = ?",
				String.class,
				paymentId);
		String draftStatus = jdbcTemplate.queryForObject(
				"select status from order_drafts where public_id = ?",
				String.class,
				draftId);
		Integer stockAfterFail = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		assertThat(reservationStatus).isEqualTo("RELEASED");
		assertThat(orderStatus).isEqualTo("PAYMENT_FAILED");
		assertThat(paymentStatus).isEqualTo("FAILED");
		assertThat(draftStatus).isEqualTo("CANCELLED");
		assertThat(stockAfterFail).isEqualTo(6);

		mockMvc.perform(post("/api/v1/webhooks/mock-payment")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Signature", hmacSha256Hex(payload, WEBHOOK_SECRET))
				.content(payload))
				.andExpect(status().isOk());

		Integer webhookEventCount = jdbcTemplate.queryForObject(
				"select count(*) from payment_webhook_events where provider = 'MOCK' and event_id = ?",
				Integer.class,
				eventId);
		assertThat(webhookEventCount).isEqualTo(1);
	}

	@Test
	void userCanRequestRefundAndWebhookMarksOrderRefunded() throws Exception {
		Long productId = insertProduct("SKU-CHECK-6", "Webhook Refund Hoodie", 4);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":1}
						""".formatted(productId)))
				.andExpect(status().isOk());

		String reserveBody = mockMvc.perform(post("/api/v1/checkout/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String draftId = extractField(reserveBody, DRAFT_ID_PATTERN);
		Long addressId = createAddressAndReturnId(userAccess, "Refund Home");

		String payBody = mockMvc.perform(post("/api/v1/checkout/drafts/{draftId}/pay", draftId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.header("Idempotency-Key", "idem-refund-" + draftId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"addressId":%d}
						""".formatted(addressId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String paymentId = extractField(payBody, PAYMENT_ID_PATTERN);
		String orderId = extractField(payBody, ORDER_ID_PATTERN);

		mockMvc.perform(post("/api/v1/payments/{paymentId}/confirm-mock", paymentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk());

		String refundBody = mockMvc.perform(post("/api/v1/orders/{orderId}/refund", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"reason":"changed mind"}
						"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String refundId = extractField(refundBody, REFUND_ID_PATTERN);

		String orderStatusAfterRefundReq = jdbcTemplate.queryForObject(
				"select status from orders where public_id = ?",
				String.class,
				orderId);
		String refundStatusAfterReq = jdbcTemplate.queryForObject(
				"select status from refunds where public_id = ?",
				String.class,
				refundId);
		assertThat(orderStatusAfterRefundReq).isEqualTo("REFUND_PENDING");
		assertThat(refundStatusAfterReq).isEqualTo("PENDING");

		String eventId = "evt_ref_succ_" + refundId;
		String payload = """
				{"eventId":"%s","type":"refund.succeeded","refundId":"%s"}
				""".formatted(eventId, refundId).trim();
		mockMvc.perform(post("/api/v1/webhooks/mock-payment")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Signature", hmacSha256Hex(payload, WEBHOOK_SECRET))
				.content(payload))
				.andExpect(status().isOk());

		String orderStatusAfterWebhook = jdbcTemplate.queryForObject(
				"select status from orders where public_id = ?",
				String.class,
				orderId);
		String refundStatusAfterWebhook = jdbcTemplate.queryForObject(
				"select status from refunds where public_id = ?",
				String.class,
				refundId);
		assertThat(orderStatusAfterWebhook).isEqualTo("REFUNDED");
		assertThat(refundStatusAfterWebhook).isEqualTo("SUCCEEDED");

		mockMvc.perform(post("/api/v1/webhooks/mock-payment")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Signature", hmacSha256Hex(payload, WEBHOOK_SECRET))
				.content(payload))
				.andExpect(status().isOk());

		Integer webhookEventCount = jdbcTemplate.queryForObject(
				"select count(*) from payment_webhook_events where provider = 'MOCK' and event_id = ?",
				Integer.class,
				eventId);
		assertThat(webhookEventCount).isEqualTo(1);
	}

	@Test
	void adminCanListAndFilterOrdersRefundsAndWebhookEvents() throws Exception {
		Long productId = insertProduct("SKU-CHECK-7", "Admin Query Jacket", 8);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");
		String adminAccess = loginAndGetAccessToken("admin@pehlione.com", "admin123");

		mockMvc.perform(post("/api/v1/cart/items")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":1}
						""".formatted(productId)))
				.andExpect(status().isOk());

		String reserveBody = mockMvc.perform(post("/api/v1/checkout/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String draftId = extractField(reserveBody, DRAFT_ID_PATTERN);
		Long addressId = createAddressAndReturnId(userAccess, "Admin Ops");

		String payBody = mockMvc.perform(post("/api/v1/checkout/drafts/{draftId}/pay", draftId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.header("Idempotency-Key", "idem-admin-list-" + draftId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"addressId":%d}
						""".formatted(addressId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String paymentId = extractField(payBody, PAYMENT_ID_PATTERN);
		String orderId = extractField(payBody, ORDER_ID_PATTERN);

		mockMvc.perform(post("/api/v1/payments/{paymentId}/confirm-mock", paymentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk());

		String refundBody = mockMvc.perform(post("/api/v1/orders/{orderId}/refund", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"reason":"query test"}
						"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String refundId = extractField(refundBody, REFUND_ID_PATTERN);

		String adminOrdersBody = mockMvc.perform(get("/api/v1/admin/orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
				.param("status", "REFUND_PENDING")
				.param("email", "user@pehlione.com")
				.param("size", "20"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(adminOrdersBody).contains(orderId);
		assertThat(adminOrdersBody).contains("user@pehlione.com");

		String adminOrderDetailBody = mockMvc.perform(get("/api/v1/admin/orders/{orderId}", orderId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(adminOrderDetailBody).contains("\"shippingAddress\":");
		assertThat(adminOrderDetailBody).contains("\"items\":[");

		String adminRefundsBody = mockMvc.perform(get("/api/v1/admin/refunds")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
				.param("status", "PENDING")
				.param("orderId", orderId))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(adminRefundsBody).contains(refundId);

		String eventId = "evt_admin_" + refundId;
		String payload = """
				{"eventId":"%s","type":"refund.failed","refundId":"%s","error":"provider timeout"}
				""".formatted(eventId, refundId).trim();
		mockMvc.perform(post("/api/v1/webhooks/mock-payment")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Signature", hmacSha256Hex(payload, WEBHOOK_SECRET))
				.content(payload))
				.andExpect(status().isOk());

		String adminWebhookBody = mockMvc.perform(get("/api/v1/admin/webhook-events")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
				.param("provider", "MOCK")
				.param("eventId", "evt_admin_"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(adminWebhookBody).contains(eventId);

		String orderStatus = jdbcTemplate.queryForObject(
				"select status from orders where public_id = ?",
				String.class,
				orderId);
		String refundStatus = jdbcTemplate.queryForObject(
				"select status from refunds where public_id = ?",
				String.class,
				refundId);
		assertThat(orderStatus).isEqualTo("PAID");
		assertThat(refundStatus).isEqualTo("FAILED");
	}

	private Long insertProduct(String sku, String name, int stockQuantity) {
		jdbcTemplate.update(
				"""
						insert into products (sku, name, description, price, currency, stock_quantity, status)
						values (?, ?, ?, ?, ?, ?, ?)
						""",
				sku,
				name,
				"checkout test",
				new BigDecimal("19.99"),
				"EUR",
				stockQuantity,
				"ACTIVE");
		return jdbcTemplate.queryForObject("select id from products where sku = ?", Long.class, sku);
	}

	private String loginAndGetAccessToken(String email, String password) throws Exception {
		String body = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s"}
						""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return extractField(body, ACCESS_TOKEN_PATTERN);
	}

	private Long createAddressAndReturnId(String accessToken, String label) throws Exception {
		String body = mockMvc.perform(post("/api/v1/addresses")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "label":"%s",
						  "fullName":"Ali Veli",
						  "phone":"+49 111 222",
						  "line1":"Street 1",
						  "line2":"Apt 3",
						  "city":"Berlin",
						  "state":"Berlin",
						  "postalCode":"10115",
						  "countryCode":"DE",
						  "makeDefault":true
						}
						""".formatted(label)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return Long.parseLong(extractField(body, ADDRESS_ID_PATTERN));
	}

	private String extractField(String body, Pattern pattern) {
		Matcher matcher = pattern.matcher(body);
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private String hmacSha256Hex(String payload, String secret) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder(digest.length * 2);
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private void resetState() {
		jdbcTemplate.update("delete from payment_webhook_events");
		jdbcTemplate.update("delete from refunds");
		jdbcTemplate.update("delete from payment_intents");
		jdbcTemplate.update("delete from shipments");
		jdbcTemplate.update("delete from order_shipping_addresses");
		jdbcTemplate.update("delete from order_items");
		jdbcTemplate.update("delete from orders");
		jdbcTemplate.update("delete from order_draft_items");
		jdbcTemplate.update("delete from order_drafts");
		jdbcTemplate.update("delete from user_addresses");
		jdbcTemplate.update("delete from cart_items");
		jdbcTemplate.update("delete from inventory_events");
		jdbcTemplate.update("delete from inventory_reservations");
		jdbcTemplate.update("delete from product_categories");
		jdbcTemplate.update("delete from products");
		jdbcTemplate.update("delete from categories");
		jdbcTemplate.update("""
				update users
				set locked = false, locked_at = null, lock_reason = null
				where email in ('user@pehlione.com', 'admin@pehlione.com')
				""");
	}
}
