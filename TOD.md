Tamam — **Product 11 = Order Transition Guard (tek yerde) + Admin list/filter endpoint’leri (orders + refunds + webhook events)**.

Aşağıdaki adımda iki şeyi “enterprise düzeyine” çekiyoruz:

1. **Order status değişimleri artık tek merkezden kontrol edilecek** (state machine / guard).
2. Admin tarafında **listeleme + filtreleme** (paging’li) endpoint’leri gelecek.

---

# A) Order Transition Guard (tek yerde)

## A1) OrderTransitionService

**`src/main/java/com/pehlione/web/order/OrderTransitionService.java`**

```java
package com.pehlione.web.order;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;

@Service
public class OrderTransitionService {

    // Allowed transitions
    private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED),
            OrderStatus.PAID,            EnumSet.of(OrderStatus.SHIPPED, OrderStatus.REFUND_PENDING, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED,         EnumSet.of(OrderStatus.FULFILLED),
            OrderStatus.REFUND_PENDING,  EnumSet.of(OrderStatus.REFUNDED, OrderStatus.PAID), // refund failed -> back to PAID
            OrderStatus.PAYMENT_FAILED,  EnumSet.of(OrderStatus.CANCELLED),

            // terminal states
            OrderStatus.CANCELLED,       EnumSet.noneOf(OrderStatus.class),
            OrderStatus.FULFILLED,       EnumSet.noneOf(OrderStatus.class),
            OrderStatus.REFUNDED,        EnumSet.noneOf(OrderStatus.class),

            // backward compat: eski PLACED varsa "PAID gibi" davran
            OrderStatus.PLACED,          EnumSet.of(OrderStatus.SHIPPED, OrderStatus.REFUND_PENDING, OrderStatus.CANCELLED)
    );

    public void assertTransition(OrderStatus from, OrderStatus to, String context) {
        if (from == to) return;
        EnumSet<OrderStatus> allowed = ALLOWED.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ApiErrorCode.CONFLICT,
                    "Invalid order transition: " + from + " -> " + to + (context == null ? "" : (" (" + context + ")"))
            );
        }
    }

    public void transition(Order order, OrderStatus to, String context) {
        OrderStatus from = order.getStatus();
        if (from == to) return; // idempotent
        assertTransition(from, to, context);
        order.setStatus(to);
    }
}
```

---

## A2) Servislerde statü set etmeyi buna bağla (minimal patch)

### FulfillmentService

**`src/main/java/com/pehlione/web/fulfillment/FulfillmentService.java`**

* Constructor’a `OrderTransitionService` ekle.
* `o.setStatus(...)` olan yerleri `transitionService.transition(...)` yap.

Örnek patch (ilgili kısımlar):

```java
import com.pehlione.web.order.OrderTransitionService;

// field
private final OrderTransitionService transitionService;

public FulfillmentService(OrderRepository orderRepo, ShipmentRepository shipmentRepo, AuditService auditService,
                          OrderTransitionService transitionService) {
    this.orderRepo = orderRepo;
    this.shipmentRepo = shipmentRepo;
    this.auditService = auditService;
    this.transitionService = transitionService;
}

// ship(...) içinde:
transitionService.transition(o, OrderStatus.SHIPPED, "admin-ship");

// deliver(...) içinde:
transitionService.transition(o, OrderStatus.FULFILLED, "admin-deliver");

// cancel(...) içinde:
transitionService.transition(o, OrderStatus.CANCELLED, "admin-cancel");
```

> Mevcut “PAID değilse ship olmaz” kontrolünü istersen kaldırabilirsin; guard zaten engelliyor.

### PaymentWebhookService (Product 10)

**`src/main/java/com/pehlione/web/webhook/PaymentWebhookService.java`**

* Constructor’a `OrderTransitionService` ekle.
* `o.setStatus(...)` satırlarını transition ile değiştir.

```java
import com.pehlione.web.order.OrderTransitionService;

private final OrderTransitionService transitionService;

public PaymentWebhookService(..., OrderTransitionService transitionService, ...) {
   ...
   this.transitionService = transitionService;
}

// payment succeeded:
transitionService.transition(o, OrderStatus.PAID, "webhook-payment-succeeded");

// payment failed:
transitionService.transition(o, OrderStatus.PAYMENT_FAILED, "webhook-payment-failed");

// refund succeeded:
transitionService.transition(o, OrderStatus.REFUNDED, "webhook-refund-succeeded");

// refund failed:
transitionService.transition(o, OrderStatus.PAID, "webhook-refund-failed");
```

### PaymentService refund create (Product 10)

**`src/main/java/com/pehlione/web/payment/PaymentService.java`**

* Refund başlatırken `order.setStatus(REFUND_PENDING)` yerine transition kullan.

```java
import com.pehlione.web.order.OrderTransitionService;

// field
private final OrderTransitionService transitionService;

// ctor param + assign

// refund create:
transitionService.transition(order, com.pehlione.web.order.OrderStatus.REFUND_PENDING, "user-refund-request");
```

---

# B) Admin list/filter endpoint’leri (Orders + Refunds + Webhook Events)

Burada en temiz yol: **Specifications**. Paging’li, dinamik filtre.

---

## B1) OrderRepository: Specification destekle

**`src/main/java/com/pehlione/web/order/OrderRepository.java`**

```java
package com.pehlione.web.order;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "shipments", "shippingAddress"})
    Optional<Order> findByPublicIdAndUserId(String publicId, Long userId);

    // Admin list: user join ile N+1 olmasın
    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAll(org.springframework.data.jpa.domain.Specification<Order> spec, Pageable pageable);

    // Admin detail (full)
    @EntityGraph(attributePaths = {"user", "items", "shipments", "shippingAddress"})
    Optional<Order> findByPublicId(String publicId);
}
```

> `findByPublicId` daha önce yoksa ekle. (User detail için zaten vardı.)

---

## B2) OrderSpecifications

**`src/main/java/com/pehlione/web/order/OrderSpecifications.java`**

```java
package com.pehlione.web.order;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class OrderSpecifications {
    private OrderSpecifications() {}

    public static Specification<Order> statusEq(OrderStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> userEmailLike(String email) {
        return (root, q, cb) -> {
            if (email == null || email.isBlank()) return cb.conjunction();
            var userJoin = root.join("user");
            return cb.like(cb.lower(userJoin.get("email")), "%" + email.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Order> orderIdLike(String qstr) {
        return (root, q, cb) -> {
            if (qstr == null || qstr.isBlank()) return cb.conjunction();
            String s = "%" + qstr.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("publicId")), s);
        };
    }

    public static Specification<Order> createdFrom(Instant from) {
        return (root, q, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Order> createdTo(Instant to) {
        return (root, q, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
```

---

## B3) Admin Orders API

### DTO

**`src/main/java/com/pehlione/web/api/admin/AdminOrderDtos.java`**

```java
package com.pehlione.web.api.admin;

import com.pehlione.web.api.order.OrderDtos;
import com.pehlione.web.order.Order;
import com.pehlione.web.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AdminOrderDtos {

    public record AdminOrderSummary(
            String orderId,
            OrderStatus status,
            String userEmail,
            String currency,
            BigDecimal totalAmount,
            Instant createdAt
    ) {
        public static AdminOrderSummary from(Order o) {
            return new AdminOrderSummary(
                    o.getPublicId(),
                    o.getStatus(),
                    o.getUser().getEmail(),
                    o.getCurrency(),
                    o.getTotalAmount(),
                    o.getCreatedAt()
            );
        }
    }

    public record AdminOrderDetail(
            String orderId,
            OrderStatus status,
            String userEmail,
            String currency,
            BigDecimal totalAmount,
            Instant createdAt,
            OrderDtos.ShippingAddressInfo shippingAddress,
            List<OrderDtos.OrderItemResponse> items,
            List<OrderDtos.ShipmentInfo> shipments
    ) {
        public static AdminOrderDetail from(Order o) {
            var sa = o.getShippingAddress();
            OrderDtos.ShippingAddressInfo shipAddr = (sa == null) ? null :
                    new OrderDtos.ShippingAddressInfo(
                            sa.getFullName(), sa.getPhone(), sa.getLine1(), sa.getLine2(),
                            sa.getCity(), sa.getState(), sa.getPostalCode(), sa.getCountryCode()
                    );

            var items = o.getItems().stream().map(OrderDtos.OrderItemResponse::from).toList();
            var ships = o.getShipments().stream().map(s ->
                    new OrderDtos.ShipmentInfo(
                            s.getPublicId(),
                            s.getStatus().name(),
                            s.getCarrier(),
                            s.getTrackingNumber(),
                            s.getShippedAt(),
                            s.getDeliveredAt()
                    )
            ).toList();

            return new AdminOrderDetail(
                    o.getPublicId(), o.getStatus(), o.getUser().getEmail(),
                    o.getCurrency(), o.getTotalAmount(), o.getCreatedAt(),
                    shipAddr, items, ships
            );
        }
    }
}
```

> Not: `OrderDtos.ShippingAddressInfo` ve `OrderDtos.ShipmentInfo` Product 9/8’de eklemiştik.

### Controller

**`src/main/java/com/pehlione/web/api/admin/AdminOrderController.java`**

```java
package com.pehlione.web.api.admin;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.order.*;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import static com.pehlione.web.api.admin.AdminOrderDtos.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderRepository repo;

    public AdminOrderController(OrderRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Page<AdminOrderSummary> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable
    ) {
        var spec = OrderSpecifications.statusEq(status)
                .and(OrderSpecifications.userEmailLike(email))
                .and(OrderSpecifications.orderIdLike(query))
                .and(OrderSpecifications.createdFrom(from))
                .and(OrderSpecifications.createdTo(to));

        return repo.findAll(spec, pageable).map(AdminOrderSummary::from);
    }

    @GetMapping("/{orderId}")
    public AdminOrderDetail get(@PathVariable String orderId) {
        Order o = repo.findByPublicId(orderId)
                .orElseThrow(() -> new ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        ApiErrorCode.NOT_FOUND,
                        "Order not found"
                ));
        return AdminOrderDetail.from(o);
    }
}
```

**Filtre örnekleri**

* `GET /api/v1/admin/orders?status=PAID&page=0&size=20`
* `GET /api/v1/admin/orders?email=gmail.com`
* `GET /api/v1/admin/orders?from=2026-02-01T00:00:00Z&to=2026-02-17T23:59:59Z`

---

## B4) Admin Refunds list/filter

### RefundRepository: spec destekle

**`src/main/java/com/pehlione/web/payment/RefundRepository.java`**

```java
package com.pehlione.web.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long>, JpaSpecificationExecutor<Refund> {
    Optional<Refund> findByPublicId(String publicId);
}
```

### RefundSpecifications

**`src/main/java/com/pehlione/web/payment/RefundSpecifications.java`**

```java
package com.pehlione.web.payment;

import org.springframework.data.jpa.domain.Specification;

public final class RefundSpecifications {
    private RefundSpecifications() {}

    public static Specification<Refund> statusEq(RefundStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Refund> orderIdEq(String orderPublicId) {
        return (root, q, cb) -> {
            if (orderPublicId == null || orderPublicId.isBlank()) return cb.conjunction();
            var orderJoin = root.join("order");
            return cb.equal(orderJoin.get("publicId"), orderPublicId.trim());
        };
    }

    public static Specification<Refund> userEmailLike(String email) {
        return (root, q, cb) -> {
            if (email == null || email.isBlank()) return cb.conjunction();
            var userJoin = root.join("user");
            return cb.like(cb.lower(userJoin.get("email")), "%" + email.trim().toLowerCase() + "%");
        };
    }
}
```

### AdminRefundController

**`src/main/java/com/pehlione/web/api/admin/AdminRefundController.java`**

```java
package com.pehlione.web.api.admin;

import com.pehlione.web.payment.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/refunds")
public class AdminRefundController {

    private final RefundRepository repo;

    public AdminRefundController(RefundRepository repo) {
        this.repo = repo;
    }

    public record RefundSummary(
            String refundId,
            RefundStatus status,
            String userEmail,
            String orderId,
            String currency,
            BigDecimal amount,
            Instant createdAt
    ) {
        static RefundSummary from(Refund r) {
            return new RefundSummary(
                    r.getPublicId(),
                    r.getStatus(),
                    r.getUser().getEmail(),
                    r.getOrder().getPublicId(),
                    r.getCurrency(),
                    r.getAmount(),
                    r.getCreatedAt()
            );
        }
    }

    @GetMapping
    public Page<RefundSummary> list(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String orderId,
            Pageable pageable
    ) {
        var spec = RefundSpecifications.statusEq(status)
                .and(RefundSpecifications.userEmailLike(email))
                .and(RefundSpecifications.orderIdEq(orderId));

        return repo.findAll(spec, pageable).map(RefundSummary::from);
    }
}
```

---

## B5) Admin Webhook events list

### Repository: spec destekle

**`src/main/java/com/pehlione/web/webhook/PaymentWebhookEventRepository.java`**

```java
package com.pehlione.web.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long>,
        JpaSpecificationExecutor<PaymentWebhookEvent> {

    Optional<PaymentWebhookEvent> findByProviderAndEventId(String provider, String eventId);
}
```

### Specs

**`src/main/java/com/pehlione/web/webhook/WebhookEventSpecifications.java`**

```java
package com.pehlione.web.webhook;

import org.springframework.data.jpa.domain.Specification;

public final class WebhookEventSpecifications {
    private WebhookEventSpecifications() {}

    public static Specification<PaymentWebhookEvent> providerEq(String provider) {
        return (root, q, cb) -> (provider == null || provider.isBlank()) ? cb.conjunction()
                : cb.equal(root.get("provider"), provider.trim());
    }

    public static Specification<PaymentWebhookEvent> eventIdLike(String eventId) {
        return (root, q, cb) -> (eventId == null || eventId.isBlank()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("eventId")), "%" + eventId.trim().toLowerCase() + "%");
    }
}
```

### Controller

**`src/main/java/com/pehlione/web/api/admin/AdminWebhookEventController.java`**

```java
package com.pehlione.web.api.admin;

import com.pehlione.web.webhook.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/webhook-events")
public class AdminWebhookEventController {

    private final PaymentWebhookEventRepository repo;

    public AdminWebhookEventController(PaymentWebhookEventRepository repo) {
        this.repo = repo;
    }

    public record WebhookEventRow(String provider, String eventId, String payloadHash, Instant receivedAt) {
        static WebhookEventRow from(PaymentWebhookEvent e) {
            return new WebhookEventRow(e.getProvider(), e.getEventId(), e.getPayloadHash(), e.getReceivedAt());
        }
    }

    @GetMapping
    public Page<WebhookEventRow> list(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String eventId,
            Pageable pageable
    ) {
        var spec = WebhookEventSpecifications.providerEq(provider)
                .and(WebhookEventSpecifications.eventIdLike(eventId));
        return repo.findAll(spec, pageable).map(WebhookEventRow::from);
    }
}
```

---

# C) Security

`/api/v1/admin/**` zaten ROLE_ADMIN korumasında olmalı. Ek bir şey yok.

---

# D) Hızlı test

* Orders:

  * `GET /api/v1/admin/orders?status=PAID&page=0&size=20`
* Refunds:

  * `GET /api/v1/admin/refunds?status=PENDING`
* Webhooks:

  * `GET /api/v1/admin/webhook-events?provider=MOCK`

---

## Product 12 (sonraki)

Bundan sonra en mantıklı iki adım:

1. **Admin “Order Ops”**: manual payment mark, resend shipment mail, partial refund, etc.
2. **Observability**: metrics + structured logs + tracing (actuator/metrics) + audit log endpoint.

“Product 12” dersen varsayılan olarak **Observability (Actuator + metrics + audit endpoint)** ile devam edeyim.
