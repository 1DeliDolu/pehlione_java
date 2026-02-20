# Pehlione Web API

A full-featured e-commerce backend built with **Spring Boot 4** and **Java 21**.  
It exposes two OpenAPI-documented REST surfaces — a **public/user API** and an **admin API** — backed by MySQL, JWT authentication, JDBC sessions, and Bucket4j rate limiting.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Database](#database)
  - [Mail (MailHog)](#mail-mailhog)
  - [Run the Application](#run-the-application)
- [Configuration](#configuration)
- [API Overview](#api-overview)
  - [Public / User API](#public--user-api)
  - [Admin API](#admin-api)
- [Authentication](#authentication)
- [Rate Limiting](#rate-limiting)
- [TypeScript Clients](#typescript-clients)
- [Testing](#testing)
- [Build](#build)
- [Port Conflicts](#port-conflicts)

---

## Features

- **JWT authentication** — short-lived access tokens (15 min) with rotating refresh tokens (7 days) delivered via `HttpOnly` cookie
- **JDBC session store** — server-side session management persisted in MySQL
- **Flyway migrations** — versioned schema evolution (V2–V24)
- **Product catalogue** — CRUD, categories, multi-image support with ordering
- **Shopping cart** — per-user session cart
- **Checkout flow** — inventory reservation → draft order → payment intent → order submission
- **Payment** — mock provider with confirm/fail simulation and idempotency key support
- **Order management** — order lifecycle (PENDING_PAYMENT → PAID → SHIPPED → FULFILLED / CANCELLED / REFUNDED)
- **Fulfillment** — admin ship / deliver / cancel actions
- **Refunds** — user-initiated refund requests, admin refund listing
- **Inventory** — stock reservation, consume and release operations; admin restock/adjust
- **Address book** — user shipping addresses with default address support
- **Session management** — list active sessions, revoke individual or all sessions, rename devices
- **Webhook events** — payment provider webhook ingestion and admin query
- **Department / tier access control** — role-based dashboards (IT, HR, Finance, Marketing, Support, Process) and customer tier endpoints
- **Rate limiting** — Bucket4j + Caffeine with configurable per-endpoint policies
- **Audit log** — security event tracking for auth actions
- **Email** — transactional mail (password reset) via Spring Mail; local dev uses MailHog
- **OpenAPI / Swagger UI** — live interactive documentation at `/swagger-ui.html`
- **TypeScript API clients** — auto-generated `ts-public` and `ts-admin` packages bundled during the Maven build

---

## Tech Stack

| Layer            | Technology                                        |
| ---------------- | ------------------------------------------------- |
| Language         | Java 21                                           |
| Framework        | Spring Boot 4                                     |
| Web              | Spring MVC                                        |
| Security         | Spring Security 6, OAuth2 Resource Server (JWT)   |
| Persistence      | Spring Data JPA, Hibernate, MySQL 8               |
| Migrations       | Flyway                                            |
| Sessions         | Spring Session JDBC                               |
| Rate Limiting    | Bucket4j 8 + Caffeine                             |
| Templating       | Thymeleaf                                         |
| Validation       | Jakarta Validation                                |
| Docs             | SpringDoc OpenAPI 3                               |
| Build            | Maven (Maven Wrapper)                             |
| Frontend clients | Node 22 / npm 10 (via frontend-maven-plugin)      |
| Testing          | JUnit 5, Spring Boot Test, Testcontainers (MySQL) |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/pehlione/web/
│   │   ├── WebApplication.java        # Entry point
│   │   ├── auth/                      # JWT, refresh tokens, password reset
│   │   ├── audit/                     # Security audit events
│   │   ├── cart/                      # Shopping cart
│   │   ├── category/                  # Product categories
│   │   ├── checkout/                  # Checkout drafts & reservation
│   │   ├── config/                    # Security, CORS, MVC config
│   │   ├── controller/                # Shared/generic controllers
│   │   ├── fulfillment/               # Admin shipping & delivery
│   │   ├── inventory/                 # Stock reservation
│   │   ├── mail/                      # Email service
│   │   ├── notification/              # Order notification service
│   │   ├── openapi/                   # OpenAPI customisation
│   │   ├── order/                     # Order domain
│   │   ├── payment/                   # Payment intents, webhooks
│   │   ├── product/                   # Product catalogue & images
│   │   ├── security/                  # Rate limiting, JWT filters
│   │   ├── user/                      # User profile & addresses
│   │   └── webhook/                   # Incoming webhook handling
│   └── resources/
│       ├── application.properties
│       ├── db/migration/              # Flyway SQL scripts (V2–V24)
│       ├── static/                    # Static web assets
│       └── templates/                 # Thymeleaf templates
├── test/
│   └── java/com/pehlione/web/         # Integration tests (Testcontainers)
api-contract/
│   ├── openapi-public.json            # Public API contract
│   └── openapi-admin.json             # Admin API contract
clients/
│   ├── ts-public/                     # Generated TypeScript client (public)
│   └── ts-admin/                      # Generated TypeScript client (admin)
```

---

## Prerequisites

- **Java 21** (JDK)
- **MySQL 8** running on `localhost:3306` with a database named `pehlione_db`
- **Docker** (optional, for MailHog)
- Maven Wrapper (`./mvnw`) — no separate Maven installation needed

---

## Getting Started

### Database

Create the database before first run. Flyway runs automatically on startup:

```sql
CREATE DATABASE pehlione_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Default credentials used by `application.properties`:

```
username: root
password: D0cker!
```

Override them with environment variables or a profile-specific properties file.

### Mail (MailHog)

The application sends transactional emails (password reset). For local development use MailHog as a mock SMTP server:

```bash
# Start
docker run -d --name mailhog --restart unless-stopped \
  -p 1025:1025 -p 8025:8025 mailhog/mailhog:v1.0.1

# Web UI
open http://localhost:8025

# Stop / Start
docker stop mailhog
docker start mailhog
```

### Run the Application

```bash
# Development mode (using Maven Wrapper)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Alternative JVM argument style
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"
```

The API is available at: **http://localhost:8083**  
Swagger UI: **http://localhost:8083/swagger-ui.html**

---

## Configuration

All configuration lives in [src/main/resources/application.properties](src/main/resources/application.properties).  
Key properties:

| Property                             | Default                                   | Description                                 |
| ------------------------------------ | ----------------------------------------- | ------------------------------------------- |
| `server.port`                        | `8083`                                    | HTTP port                                   |
| `spring.datasource.url`              | `jdbc:mysql://localhost:3306/pehlione_db` | MySQL connection                            |
| `app.jwt.secret`                     | _(change me)_                             | HMAC secret for signing JWTs (min 32 chars) |
| `app.jwt.access-minutes`             | `15`                                      | Access token TTL                            |
| `app.jwt.refresh-days`               | `7`                                       | Refresh token TTL                           |
| `app.jwt.issuer`                     | `pehlione`                                | JWT issuer claim                            |
| `app.security.refresh-cookie.secure` | `false`                                   | Set `true` in production (HTTPS)            |
| `app.security.refresh-reuse.action`  | `LOCK_ACCOUNT`                            | What to do on refresh token reuse           |
| `app.public-base-url`                | `http://localhost:8083`                   | Used in email links                         |
| `app.images.storage-root`            | `uploads/product-images`                  | Local image upload directory                |
| `app.ratelimit.enabled`              | `true`                                    | Toggle rate limiting                        |
| `app.webhooks.mock.secret`           | _(change me)_                             | Signature secret for mock webhook endpoint  |
| `spring.mail.host` / `port`          | `localhost:1025`                          | SMTP server (MailHog locally)               |

> **Never commit real secrets.** Override sensitive values with environment variables in shared/production environments.

---

## API Overview

Full interactive documentation is available via **Swagger UI** at `/swagger-ui.html` once the application is running.  
The raw OpenAPI JSON contracts are in the [api-contract/](api-contract/) directory.

### Public / User API

Base path: `/api/v1`  
Authentication: Bearer JWT (except login, register, product listing, categories)

| Tag                 | Endpoints                                                                                                                                                                             |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Auth**            | `POST /auth/login`, `POST /auth/logout`, `POST /auth/refresh`                                                                                                                         |
| **Auth - Password** | `POST /auth/password/forgot`, `POST /auth/password/reset`                                                                                                                             |
| **Profile**         | `GET /me`, `POST /me/password`                                                                                                                                                        |
| **Sessions**        | `GET /sessions`, `POST /sessions/{id}/revoke`, `POST /sessions/revoke-all`, `PATCH /sessions/{id}`                                                                                    |
| **Products**        | `GET /products`, `GET /products/{id}`, `POST /products`, `PUT /products/{id}`, `DELETE /products/{id}`                                                                                |
| **Product Images**  | `POST /products/{id}/images`, `PUT /products/{id}/images/reorder`, `DELETE /products/{id}/images/{imageId}`                                                                           |
| **Categories**      | `GET /categories`, `POST /categories`, `PUT /categories/{id}`, `DELETE /categories/{id}`                                                                                              |
| **Cart**            | `GET /cart`, `POST /cart/items`, `DELETE /cart/items/{productId}`, `DELETE /cart`                                                                                                     |
| **Checkout**        | `POST /checkout/reserve`, `GET /checkout/drafts/{draftId}`, `POST /checkout/drafts/{draftId}/pay`, `POST /checkout/drafts/{draftId}/submit`, `POST /checkout/drafts/{draftId}/cancel` |
| **Inventory**       | `POST /inventory/reserve`, `POST /inventory/reservations/{id}/consume`, `POST /inventory/reservations/{id}/release`                                                                   |
| **Orders**          | `GET /orders`, `GET /orders/{orderId}`, `POST /orders/{orderId}/refund`                                                                                                               |
| **Payments**        | `GET /payments/{id}`, `POST /payments/{id}/confirm-mock`, `POST /payments/{id}/fail-mock`                                                                                             |
| **Addresses**       | `GET /addresses`, `POST /addresses`, `PUT /addresses/{id}`, `DELETE /addresses/{id}`, `POST /addresses/{id}/default`                                                                  |

### Admin API

Base path: `/api/v1/admin`  
Requires `ROLE_ADMIN` (Bearer JWT).

| Tag                        | Endpoints                                                                                                          |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| **Admin - Orders**         | `GET /admin/orders`, `GET /admin/orders/{orderId}`                                                                 |
| **Admin - Fulfillment**    | `POST /admin/orders/{orderId}/ship`, `POST /admin/orders/{orderId}/deliver`, `POST /admin/orders/{orderId}/cancel` |
| **Admin - Inventory**      | `POST /admin/inventory/products/{id}/restock`, `POST /admin/inventory/products/{id}/adjust`                        |
| **Admin - Refunds**        | `GET /admin/refunds`                                                                                               |
| **Admin - Webhook Events** | `GET /admin/webhook-events`                                                                                        |

---

## Authentication

The application uses a **JWT + rotating refresh token** scheme:

1. `POST /api/v1/auth/login` — returns a short-lived **access token** (JSON) and sets an `HttpOnly` `refresh_token` cookie.
2. Use the access token as `Authorization: Bearer <token>` on protected endpoints.
3. When the access token expires, call `POST /api/v1/auth/refresh` (cookie sent automatically) to get a new access token and a rotated refresh token.
4. Refresh token reuse detection is enabled: a reuse attempt triggers `LOCK_ACCOUNT` by default.
5. `POST /api/v1/auth/logout` — revokes the current refresh token and clears the cookie.

---

## Rate Limiting

Powered by **Bucket4j** with an in-memory **Caffeine** cache (TTL configurable via `app.ratelimit.bucket-ttl-minutes`).

Default policies (`application.properties`):

| Policy key                 | Default     |
| -------------------------- | ----------- |
| `auth_ip_per_minute`       | 10 req/min  |
| `auth_ip_per_hour`         | 100 req/hr  |
| `api_user_per_minute`      | 120 req/min |
| `api_ip_per_minute`        | 300 req/min |
| `checkout_user_per_minute` | 20 req/min  |
| `payment_user_per_minute`  | 20 req/min  |
| `webhook_ip_per_minute`    | 120 req/min |

Exceeding a limit returns `HTTP 429` with a `Retry-After` header.

---

## TypeScript Clients

Two typed API clients are generated automatically during the Maven build (via `frontend-maven-plugin`):

| Package     | Path                                     | Covers            |
| ----------- | ---------------------------------------- | ----------------- |
| `ts-public` | [clients/ts-public/](clients/ts-public/) | Public / user API |
| `ts-admin`  | [clients/ts-admin/](clients/ts-admin/)   | Admin API         |

Both packages are built from the OpenAPI contracts in [api-contract/](api-contract/).

To regenerate clients manually:

```bash
./scripts/generate-ts-clients.sh
```

To update the OpenAPI contract from a running server:

```bash
./scripts/update-openapi-contract.sh
```

---

## Testing

Tests use **JUnit 5**, **Spring Boot Test**, and **Testcontainers** (spins up a real MySQL container).

```bash
# Run all tests
./mvnw test
```

Test classes follow the `*Tests.java` naming convention and are located under `src/test/java/com/pehlione/web/`.

---

## Build

```bash
# Quick build (skip tests)
./mvnw -DskipTests package

# Full clean build (includes tests)
./mvnw clean package

# Run the packaged JAR
java -jar target/web-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

The frontend-maven-plugin installs Node 22 / npm 10 locally under `target/node` on first run and builds the TypeScript clients. Use `-Dfrontend.skip=true` to skip the Node build step.

---

## Port Conflicts

If the application fails to start with `Port 8083 was already in use`:

```bash
# Find the process
ss -ltnp | grep ':8083'
# or
lsof -i :8083 -sTCP:LISTEN -n -P

# Kill it (replace PID)
kill <PID>

# Force kill if needed
kill -9 <PID>
```
