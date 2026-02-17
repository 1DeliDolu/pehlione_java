Tamam — **Product 16 = OpenAPI’yi “contract-grade” yapıyoruz**: endpoint’lere `@Operation/@ApiResponses`, örnek request/response’lar, enum dokümantasyonu ve **sayfalama standardı**.

Aşağıda **direkt uygulayabileceğin** bir paket var.

---

## 1) Standard pagination: `PageResponse<T>` (Spring `Page<>` yerine)

Spring `Page<>` Swagger’da genelde “kötü” görünür. Bu yüzden tek bir response standardı yapalım:

### 1.1 DTO’lar

**`src/main/java/com/pehlione/web/api/common/PageResponse.java`**

```java
package com.pehlione.web.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponse")
public record PageResponse<T>(
        @Schema(description = "Listed items") List<T> items,
        @Schema(description = "Paging metadata") PageMeta page
) {}
```

**`src/main/java/com/pehlione/web/api/common/PageMeta.java`**

```java
package com.pehlione.web.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PageMeta")
public record PageMeta(
        @Schema(example = "0") int number,
        @Schema(example = "20") int size,
        @Schema(example = "120") long totalElements,
        @Schema(example = "6") int totalPages,
        @Schema(example = "true") boolean first,
        @Schema(example = "false") boolean last
) {}
```

### 1.2 Mapper

**`src/main/java/com/pehlione/web/api/common/PageMapper.java`**

```java
package com.pehlione.web.api.common;

import org.springframework.data.domain.Page;

public final class PageMapper {
    private PageMapper() {}

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new PageMeta(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isFirst(),
                        page.isLast()
                )
        );
    }
}
```

> Sonra controller’larda `return PageMapper.of(service.list(...).map(dto::from))` şeklinde döneceğiz.

---

## 2) Pagination param’larını Swagger’da düzgün göster (`@ParameterObject`)

Controller method’larında pageable parametresini şöyle yaz:

```java
import org.springdoc.core.annotations.ParameterObject;

public PageResponse<...> list(@ParameterObject Pageable pageable) { ... }
```

Bu sayede `page/size/sort` parametreleri Swagger’da otomatik görünür.

---

## 3) Controller’ları “contract-first” annotate et

### 3.1 Class seviyesinde Tag + Security

Örn. **OrderController**:

**`src/main/java/com/pehlione/web/api/order/OrderController.java`** (başına)

```java
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Orders", description = "User order endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
```

> Admin controller’larda `@Tag(name="Admin - Orders")` gibi ayır.

---

## 4) Örnek: Orders list + detail’i standardize et (PageResponse + annotations)

### 4.1 Orders list

```java
import com.pehlione.web.api.common.PageMapper;
import com.pehlione.web.api.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.core.annotations.ParameterObject;

@Operation(
        summary = "List my orders",
        description = "Returns the authenticated user's orders (newest first)."
)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paged order list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(ref = "#/components/schemas/ApiProblem")))
})
@GetMapping
public PageResponse<OrderSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                              @ParameterObject Pageable pageable) {
    Long userId = userRepo.findByEmail(jwt.getSubject()).orElseThrow().getId();
    var page = orderService.listForUser(userId, pageable).map(OrderSummaryResponse::from);
    return PageMapper.of(page);
}
```

### 4.2 Order detail

```java
@Operation(summary = "Get my order details")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order detail"),
        @ApiResponse(responseCode = "404", description = "Order not found",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(ref = "#/components/schemas/ApiProblem")))
})
@GetMapping("/{orderId}")
public OrderDetailResponse get(@AuthenticationPrincipal Jwt jwt,
                               @Parameter(description = "Order public id (UUID)", example = "6b7a...") 
                               @PathVariable String orderId) {
    Long userId = userRepo.findByEmail(jwt.getSubject()).orElseThrow().getId();
    return OrderDetailResponse.from(orderService.getForUser(orderId, userId));
}
```

---

## 5) Örnek: Checkout /pay endpoint’ini örneklerle dokümante et

### 5.1 DTO’ya Schema + example

**`CheckoutDtos.PayRequest`**:

```java
import io.swagger.v3.oas.annotations.media.Schema;

public record PayRequest(
        @Schema(description = "Shipping address id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long addressId
) {}
```

### 5.2 Controller annotation

```java
import io.swagger.v3.oas.annotations.media.ExampleObject;

@Operation(
        summary = "Start payment for a draft",
        description = "Creates an order in PENDING_PAYMENT, attaches shipping address snapshot, creates payment intent. Idempotent via Idempotency-Key."
)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment started",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = StartPaymentResponse.class),
                        examples = @ExampleObject(value = """
                        {"paymentId":"9f6e...","orderId":"2a1b..."}
                        """))),
        @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(ref="#/components/schemas/ApiProblem"))),
        @ApiResponse(responseCode = "409", description = "Draft not reserved/expired",
                content = @Content(mediaType = "application/problem+json",
                        schema = @Schema(ref="#/components/schemas/ApiProblem")))
})
@PostMapping("/drafts/{draftId}/pay")
public StartPaymentResponse pay(@AuthenticationPrincipal Jwt jwt,
                                @PathVariable String draftId,
                                @RequestBody PayRequest req,
                                HttpServletRequest request,
                                @RequestHeader(name="Idempotency-Key", required=false) String idempotencyKey) {
    String sid = jwt.getClaimAsString("sid");
    var res = checkoutService.startPayment(jwt.getSubject(), sid, draftId, ClientInfo.from(request), idempotencyKey, req.addressId());
    return new StartPaymentResponse(res.paymentId(), res.orderId());
}
```

---

## 6) Webhook endpoint: Swagger’da “security yok” göster (çok önemli)

Webhook controller method’una:

```java
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Operation(
        summary = "Mock payment webhook receiver",
        description = "Validates HMAC signature via X-Signature and processes the event.",
        security = { @SecurityRequirement(name = "") } // <-- bearerAuth istemesin
)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Accepted"),
        @ApiResponse(responseCode = "401", description = "Invalid signature",
                content = @Content(mediaType="application/problem+json",
                        schema=@Schema(ref="#/components/schemas/ApiProblem")))
})
@PostMapping
public void handle(...) { ... }
```

---

## 7) Enum’ları Swagger’da net göster (`@Schema`)

Örn. `OrderStatus`:

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order status lifecycle")
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    FULFILLED,
    PAYMENT_FAILED,
    REFUND_PENDING,
    REFUNDED,
    CANCELLED
}
```

Aynı şeyi `PaymentStatus`, `RefundStatus`, `ShipmentStatus` için de yap.

---

## 8) Admin list endpoint’lerinde filtre param’larını dokümante et

Örn. `/api/v1/admin/orders`:

```java
@Operation(summary = "Admin: list orders", description = "Filter by status/email/orderId/date range.")
@GetMapping
public PageResponse<AdminOrderSummary> list(
        @Parameter(description="Order status filter") @RequestParam(required=false) OrderStatus status,
        @Parameter(description="User email contains", example="gmail.com") @RequestParam(required=false) String email,
        @Parameter(description="Order id contains", example="6b7a") @RequestParam(required=false, name="q") String query,
        @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @ParameterObject Pageable pageable
) {
    var page = repo.findAll(spec, pageable).map(AdminOrderSummary::from);
    return PageMapper.of(page);
}
```

---

## 9) Minimum “done” checklist (Product 16 tamam kriteri)

* [ ] Tüm list endpoint’leri `PageResponse<T>` dönüyor (orders, admin orders, refunds, webhook-events, audit-logs)
* [ ] Her controller `@Tag` alıyor
* [ ] Secure endpoint’ler `@SecurityRequirement(bearerAuth)` alıyor; webhooks override ediyor
* [ ] Önemli endpoint’lerde request/response `@ExampleObject` var
* [ ] Enum’lar `@Schema` ile açıklanmış
* [ ] Hata responses `application/problem+json` referanslıyor (ApiProblem schema zaten Product 15’te vardı)

---

### Product 17 (sonraki)

Bir sonraki “çok değerli” adım: **OpenAPI’dan client generation** (TypeScript/Java) + contract test (Spring MockMvc + OpenAPI snapshot).
