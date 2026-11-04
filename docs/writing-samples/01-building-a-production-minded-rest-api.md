# Build a Production-Minded REST API with Spring Boot 4.1 and Java 21

**Sample type:** Tutorial / step-by-step  
**Source code:** [Product Catalog service](../../product-catalog-service)  
**Stack:** Java 21, Spring Boot 4.1.2, PostgreSQL 17, Flyway, Redis 7, Testcontainers

A REST endpoint is easy to demonstrate. The engineering work begins when the service must preserve data integrity, return predictable errors, handle concurrent updates, and remain testable outside a developer's laptop. This tutorial walks through the decisions behind the Product Catalog API in this repository.

This is a production-minded learning blueprint, not a claim that the service is ready to expose publicly: authentication, authorization, production secrets, and several operational controls remain out of scope.

## 1. Define the resource contract first

The service exposes a small product resource:

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | `/api/v1/products` | Create a product |
| GET | `/api/v1/products/{id}` | Retrieve by UUID |
| PUT | `/api/v1/products/{id}` | Update mutable fields |
| DELETE | `/api/v1/products/{id}` | Delete a product |
| GET | `/api/v1/products?page=0&size=20` | List newest products first |

The API uses UUID identifiers, treats SKU as a unique business key, and keeps SKU immutable after creation. A create request looks like this:

```json
{
  "sku": "keyboard-001",
  "name": "Mechanical Keyboard",
  "description": "Compact mechanical keyboard",
  "price": 129.99
}
```

A successful create returns `201 Created` with a `Location` header. Delete returns `204 No Content`.

## 2. Validate at the boundary, enforce invariants in the database

Request DTOs keep the external contract separate from the JPA entity. Bean Validation rejects blank names, overlong values, and prices that are missing or have more than two fractional digits.

Validation improves the client experience, but it is not a substitute for database constraints. Two requests can pass an application-level uniqueness check at nearly the same time. The database therefore remains the final authority for SKU uniqueness.

The Flyway migration defines the durable invariants:

```sql
CREATE TABLE products
(
    id          UUID PRIMARY KEY,
    sku         VARCHAR(64)    NOT NULL,
    name        VARCHAR(160)   NOT NULL,
    description VARCHAR(2000),
    price       NUMERIC(19, 2) NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price_positive CHECK (price > 0)
);

CREATE INDEX idx_products_created_at ON products (created_at DESC);
```

The service normalizes SKU values by trimming and converting them to uppercase before persistence. PostgreSQL still enforces uniqueness if concurrent requests race.

Flyway owns schema changes. Hibernate is configured to validate the schema at startup rather than silently create or mutate production tables.

## 3. Keep persistence models out of the API

The controller accepts request DTOs and returns response DTOs; it does not serialize JPA entities directly. This avoids coupling the public API to lazy-loaded relationships, persistence annotations, or future schema changes.

The service layer owns use cases and transaction boundaries. The repository layer handles persistence. This separation makes it possible to change the storage implementation without rewriting the HTTP contract.

## 4. Make concurrency behavior explicit

The product entity uses JPA's `@Version` field for optimistic locking. When two clients read the same version and attempt conflicting updates, the stale write fails rather than silently overwriting the newer value.

The exception handler translates optimistic-lock failures into HTTP `409 Conflict` using RFC 9457 Problem Details. The client should fetch the latest representation and decide whether to retry or reconcile its change; it should not blindly replay an old update.

## 5. Add cache-aside reads without hiding failure semantics

Lookup by product ID uses Redis cache-aside with a five-minute TTL. A cache miss loads the product from PostgreSQL and stores the response in Redis. Updates and deletes evict the corresponding cache entry.

The failure policy is deliberately asymmetric:

- Cache read and write failures are logged, and the database result is still returned.
- Cache eviction failures are surfaced rather than silently accepting a potentially stale cache entry.

There is an important transactional nuance: an eviction exception can reach the caller after the database transaction has committed. A 5xx response therefore does not necessarily mean the mutation was rolled back. A production deployment needs a post-commit invalidation and reconciliation strategy, such as a transactional outbox or versioned cache keys.

## 6. Test the real integration boundaries

Unit tests cover focused business behavior. Integration tests start PostgreSQL and Redis with Testcontainers, launch the application on a random port, and exercise the HTTP API.

The integration suite verifies resource creation and retrieval, duplicate-SKU conflict behavior, update/delete cache eviction, metrics, and that a development-only slow endpoint is not exposed under the default profile.

Run the checks with:

```bash
mvn -f product-catalog-service/pom.xml verify
```

Docker is required for the Testcontainers integration tests.

## 7. Run the service locally

Start the data dependencies and launch the application:

```bash
docker compose -f product-catalog-service/compose.yaml up -d postgres redis
mvn -f product-catalog-service/pom.xml spring-boot:run
```

Or start the complete stack, including the application container:

```bash
docker compose -f product-catalog-service/compose.yaml up --build -d
```

The API is available at `http://localhost:8080`. The health endpoint is `/actuator/health`.

## What this blueprint demonstrates—and what it does not

The repository includes Flyway migrations, database constraints, optimistic locking, Problem Details responses, Redis cache-aside, Testcontainers, a non-root multi-stage Docker image, Prometheus metrics, and OpenTelemetry traces visualized with Jaeger.

It does not yet implement authentication/authorization, production management-endpoint isolation, robust post-commit cache reconciliation, or a full production operations policy. Those are deployment requirements, not details to conceal in a tutorial.

**Explore the implementation:** [java-microservices-blueprints](https://github.com/FatmaEmran/java-microservices-blueprints)
