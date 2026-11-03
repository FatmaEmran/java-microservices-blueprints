# Java Microservices Blueprints

Production-minded Java and Spring Boot examples focused on the engineering details that matter in real services: API design, persistence, caching, observability, containerization, testing, and runtime optimization.

This repository is built alongside practical technical articles. Each blueprint will be runnable, tested, documented, and explicit about trade-offs.

## First blueprint: Product Catalog API

A small service built with **Java 21** and **Spring Boot 4.1.2**. It will demonstrate:

- REST API design and request validation using Spring MVC
- PostgreSQL persistence and versioned Flyway migrations
- Redis-backed cache-aside reads and deliberate cache invalidation
- Health checks and application metrics with Spring Boot Actuator
- Unit and integration tests using JUnit, Mockito, and Testcontainers
- Reproducible container builds with Docker
- Distributed tracing with Micrometer and OpenTelemetry
- GraalVM Native Image builds, with measured startup and memory comparisons

Spring Boot 4.1 adds useful material for advanced follow-up articles, including first-class Spring gRPC support and observability improvements. We will introduce those as focused examples rather than forcing unrelated concerns into the catalog service.

The implementation is incremental. A capability is not considered complete until its tests and CI checks pass.

## Current API

Base path: `/api/v1/products`

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/v1/products` | Create a product |
| GET | `/api/v1/products/{id}` | Retrieve a product by UUID |
| GET | `/api/v1/products?page=0&size=20` | List products, newest first |

Product SKUs are normalized to uppercase. SKU uniqueness is enforced both in the application and by a database constraint. Create requests validate the name, SKU, and positive monetary amount. Errors use RFC 9457 Problem Details.

Example create request:

```json
{
  "sku": "keyboard-001",
  "name": "Mechanical Keyboard",
  "description": "Compact mechanical keyboard",
  "price": 129.99
}
```

A successful create returns `201 Created` and a `Location` header for the new resource.

## Run locally

Requirements: JDK 21, Maven 3.9+, and Docker with Docker Compose.

Start PostgreSQL:

```bash
docker compose -f product-catalog-service/compose.yaml up -d
```

Run the service from the repository root:

```bash
mvn -f product-catalog-service/pom.xml spring-boot:run
```

The service uses these local defaults, which can be overridden with environment variables:

- `DB_URL=jdbc:postgresql://localhost:5432/catalog`
- `DB_USERNAME=catalog`
- `DB_PASSWORD=catalog`

Flyway applies database migrations at startup. Hibernate validates the resulting schema; it does not create or mutate tables.

Run unit and integration tests:

```bash
mvn -f product-catalog-service/pom.xml verify
```

The integration tests use Testcontainers and require Docker. GitHub Actions runs the same Maven verification on pushes and pull requests that touch the service.

## Repository layout

```text
java-microservices-blueprints/
├── .github/
│   └── workflows/
│       └── product-catalog-ci.yml
├── product-catalog-service/
│   ├── compose.yaml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fatmaemran/blueprints/catalog/
│   │   │   └── resources/db/migration/
│   │   └── test/
│   └── pom.xml
└── README.md
```

## Development status

| Capability | Status |
| --- | --- |
| Spring Boot 4.1.2 and Java 21 project setup | Implemented |
| REST API, validation, pagination, and error responses | Implemented; CI verification pending |
| PostgreSQL persistence and Flyway migration | Implemented; CI verification pending |
| Unit and Testcontainers integration tests | Implemented; CI verification pending |
| Local PostgreSQL Compose environment | Implemented |
| Redis caching and invalidation | Planned |
| Metrics and distributed tracing | Planned |
| Application container image | Planned |
| GraalVM Native Image measurements | Planned |
| CI build and quality checks | Workflow added; first run pending |

## Technical article roadmap

The repository and articles will evolve together. The initial publishing sequence is:

1. **Build a Production-Ready REST API with Spring Boot 4.1 and Java 21** — project structure, validation, persistence boundaries, error handling, and API tests.
2. **Redis Cache-Aside in Spring Boot: TTLs, Invalidation, and Failure Modes** — cache correctness, stale data, serialization, and measurable behavior.
3. **Integration Testing Spring Boot 4.1 with PostgreSQL and Testcontainers** — realistic tests without relying on a developer's local database.
4. **Distributed Tracing in Spring Boot 4.1 with OpenTelemetry** — trace context, spans, metrics, and diagnosing a slow request.
5. **Spring Boot JVM vs GraalVM Native Image** — build process, compatibility constraints, startup time, memory use, and a reproducible benchmark.

A separate short example/article can explore **Spring gRPC in Spring Boot 4.1**, where it genuinely fits a service-to-service communication use case.

### Publishing strategy

- Publish the first two technically complete articles on DEV Community to establish a public writing track record.
- Keep each article focused on one engineering problem, with runnable code and reproducible commands.
- Link every article to the exact repository tag or commit it describes.
- After building a consistent portfolio of roughly 3–5 strong articles, pitch suitable paid technical-writing programs and publications. Verify each publisher's current submission requirements before submitting.

## Why this project?

The goal is not to collect technologies. It is to show how to combine them responsibly, measure the impact of optimizations, and explain the trade-offs with reproducible examples.

## License

MIT. See [LICENSE](LICENSE) once added.
