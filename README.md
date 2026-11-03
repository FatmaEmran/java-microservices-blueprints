# Java Microservices Blueprints

Production-minded Java and Spring Boot examples focused on the engineering details that matter in real services: API design, persistence, caching, observability, containerization, testing, and runtime optimization.

This repository is built alongside practical technical articles. Each blueprint will be runnable, tested, documented, and explicit about trade-offs.

## First blueprint: Product Catalog API

A small Spring Boot service used to demonstrate:

- Java 21 and Spring Boot 3.5
- REST API design and request validation
- PostgreSQL persistence
- Redis-backed caching with explicit cache invalidation
- Health and application metrics with Spring Boot Actuator
- Multi-stage Docker builds
- Integration tests with Testcontainers
- OpenTelemetry tracing
- GraalVM Native Image build and runtime comparison

The implementation will be added incrementally. Features will only be marked complete after they are implemented and verified.

## Repository layout

```text
java-microservices-blueprints/
├── product-catalog-service/
├── docs/
│   └── decisions/
├── .github/
│   └── workflows/
└── README.md
```

## Development status

| Capability | Status |
| --- | --- |
| Project structure and build | In progress |
| REST API and validation | Planned |
| PostgreSQL persistence | Planned |
| Redis caching | Planned |
| Automated tests | Planned |
| Docker Compose | Planned |
| OpenTelemetry | Planned |
| GraalVM Native Image | Planned |

## Prerequisites

- JDK 21
- Docker Engine with Docker Compose
- Maven 3.9+ (or use the Maven Wrapper once added)

## Why this project?

The goal is not to collect technologies. It is to show how to combine them responsibly, measure the impact of optimizations, and explain the trade-offs with reproducible examples.

## License

MIT. See [LICENSE](LICENSE) once added.
