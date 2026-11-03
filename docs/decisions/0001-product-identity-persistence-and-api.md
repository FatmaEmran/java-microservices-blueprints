# ADR 0001: Product identity, persistence, and API boundaries

- **Status:** Accepted
- **Date:** 2026-11-03
- **Scope:** `product-catalog-service`

## Context

The first blueprint needs a small domain model that is realistic enough to demonstrate production concerns without turning the tutorial into a full commerce platform.

The API must provide stable resource identifiers, prevent duplicate product SKUs, validate monetary values, and keep database schema changes reproducible.

## Decisions

### Product identifiers use UUIDs

The database generates UUID identifiers for products. Clients do not choose resource IDs, and IDs do not expose insertion order.

### SKU is normalized and unique

The service trims and uppercases SKUs before persistence. A unique database constraint is the final authority.

The service also performs an existence check to return a clear conflict response in the common case. This check is not a concurrency guarantee: two requests can pass it at the same time. The database constraint closes that race, and the API maps the resulting integrity violation to HTTP 409.

### HTTP models are separate from persistence entities

The controller accepts a validated request record and returns a response record. JPA entities are not serialized directly. This keeps persistence changes from silently changing the public API.

### Flyway owns the schema

Versioned SQL migrations define the database schema. Hibernate uses `ddl-auto: validate` so it checks mappings at startup without creating or mutating production tables.

### Optimistic versioning is enabled

The entity has a JPA `@Version` field. It prepares the model for safe concurrent updates when update endpoints are introduced. The initial API only creates, reads, and lists products.

### Monetary values use decimal arithmetic

Prices use `BigDecimal` and PostgreSQL `NUMERIC(19, 2)`. Binary floating-point types are deliberately avoided for money.

### Pagination is bounded

The list endpoint defaults to 20 items and caps page size at 100 to avoid unbounded result retrieval.

## Consequences

- SKU uniqueness is guaranteed by the database, not by a preflight query alone.
- Schema changes are reviewable and version-controlled.
- API contracts can evolve independently from JPA entities.
- Optimistic locking can be demonstrated when product update behavior is added.
- Currency is intentionally omitted from this first model. A multi-currency price model would need an explicit currency code and business rules rather than an implicit assumption.

## Follow-up

When update endpoints are added, define the expected behavior for optimistic-lock conflicts and expose an appropriate HTTP response, typically 409 Conflict.
