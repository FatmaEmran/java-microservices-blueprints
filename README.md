# Java Microservices Blueprints

Production-minded Java and Spring Boot examples focused on the engineering details that matter in real services: API design, persistence, caching, observability, containerization, testing, and runtime optimization.

This repository is built alongside practical technical articles. Each blueprint will be runnable, tested, documented, and explicit about trade-offs.

## First blueprint: Product Catalog API

A small service built with **Java 21** and **Spring Boot 4.1.2**. It will demonstrate:

- REST API design and request validation using Spring MVC
- PostgreSQL persistence and schema migration
- Redis-backed cache-aside reads and deliberate cache invalidation
- Health checks and application metrics with Spring Boot Actuator
- Integration tests using Testcontainers
- Reproducible container builds with Docker
- Distributed tracing with Micrometer and OpenTelemetry
- GraalVM Native Image builds, with measured startup and memory comparisons

Spring Boot 4.1 adds useful material for advanced follow-up articles, including first-class Spring gRPC support and observability improvements. We will introduce those as focused examples rather than forcing unrelated concerns into the catalog service.

The implementation will be added incrementally. Features will only be marked complete after they are implemented and verified.

## Repository layout

```text
java-microservices-blueprints/
├── product-catalog-service/
├── docs/
│   ├── decisions/
│   └── articles/
├── .github/
│   └── workflows/
└── README.md
```

## Development status

| Capability | Status |
| --- | --- |
| Project structure and Spring Boot 4.1.2 build | In progress |
| REST API and validation | Planned |
| PostgreSQL persistence and migrations | Planned |
| Redis caching and invalidation | Planned |
| Unit and integration tests | Planned |
| Docker Compose and container image | Planned |
| Metrics and distributed tracing | Planned |
| GraalVM Native Image measurements | Planned |
| CI build and quality checks | Planned |

## Prerequisites

- JDK 21
- Docker Engine with Docker Compose
- Maven 3.9+

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
- After building a consistent portfolio of roughly 3–5 strong articles, pitch suitable paid technical-writing programs and publications. Do not assume any one publisher's program is currently accepting pitches; verify its current guidelines before submitting.

## Why this project?

The goal is not to collect technologies. It is to show how to combine them responsibly, measure the impact of optimizations, and explain the trade-offs with reproducible examples.

## License

MIT. See [LICENSE](LICENSE) once added.
