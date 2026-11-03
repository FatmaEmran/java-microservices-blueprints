# Product Catalog: Production-Readiness Review

This review separates capabilities demonstrated by the blueprint from controls that would still be required before exposing a real product service to production traffic.

## Verified in CI

The GitHub Actions workflow currently verifies:

- Maven unit and integration tests against PostgreSQL and Redis Testcontainers.
- Docker image construction.
- Full Docker Compose startup.
- Application health endpoint availability.
- Product creation through the HTTP API.
- Prometheus scraping of the custom product-creation metric.
- Jaeger receipt of application traces.
- The development-only slow endpoint is absent when the default profile is active.
- Compose stack cleanup after the smoke test.

See the [latest workflow](https://github.com/FatmaEmran/java-microservices-blueprints/actions).

## Implemented engineering controls

- PostgreSQL schema changes are managed by Flyway; Hibernate validates rather than mutates the schema.
- Product writes use a database uniqueness constraint and optimistic locking.
- API errors use RFC 9457 Problem Details.
- Redis cache entries have a five-minute TTL. Read/write failures fall back to the database result; eviction failures are surfaced.
- Metric labels are bounded; product IDs and SKUs are not used as metric dimensions.
- The container runs as a non-root user and uses a multi-stage build.
- Graceful shutdown is enabled with a 20-second shutdown phase.
- Liveness/readiness health probe support is enabled.

## Gaps before production deployment

These are deliberate follow-up items, not claims that the current sample is production-secure.

| Area | Current state | Required before production |
| --- | --- | --- |
| Authentication and authorization | Not implemented; product API is unauthenticated | Define identity, authentication, authorization, and service-to-service access policy |
| Actuator exposure | Health, info, metrics, and Prometheus are exposed on the application port | Put management endpoints on a private network or separate secured management interface; expose only what operators need |
| Cache consistency | Update/delete evict after method execution; eviction failure can surface after the database transaction has committed | Define post-commit invalidation semantics and a recovery strategy (for example, transactional outbox or versioned cache keys) |
| Concurrent writes | JPA optimistic locking is enabled; stale-write failures now map to HTTP 409 | Add a deterministic concurrent-update integration test and document client retry/version behavior |
| Resilience | No explicit database/Redis timeout budget or request-level rate limiting is configured | Set connection, query, and request timeouts; define overload and dependency-outage behavior |
| Operations | Local Prometheus and Jaeger are configured | Define production retention, access control, alerting, dashboards, sampling, and trace/metric storage |
| Container supply chain | Non-root multi-stage image; base images use mutable tags | Pin image digests, scan dependencies/images, and publish an SBOM as part of release CI |
| Configuration/secrets | Local Compose credentials are intentionally simple defaults | Use a secret manager and environment-specific configuration; never reuse local credentials |
| API protection | No rate limiting, payload-size policy, or abuse controls | Define request limits, quotas, and edge/API gateway protections |
| Data lifecycle | Initial schema and persistence are implemented | Define backup/restore, migration rollout, retention, and disaster-recovery procedures |

## Important cache failure semantic

A cache eviction exception does not automatically mean the database mutation was rolled back. Depending on transaction and cache-advisor ordering, the database write may already have committed when the caller receives an error. A production design must make this outcome explicit and provide a reliable invalidation/reconciliation mechanism. Do not blindly retry non-idempotent operations after an ambiguous 5xx response.

## Scope for the first article

The first article can accurately describe the service as a production-minded, runnable blueprint. It should not claim the service is fully production-secure or ready for public deployment. Security, operational policy, and distributed cache consistency remain explicit deployment responsibilities.
