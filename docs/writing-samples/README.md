# Technical Writing Samples

A curated set of practical engineering articles based on the runnable [Java Microservices Blueprints](https://github.com/FatmaEmran/java-microservices-blueprints) repository.

These samples are written against the implementation in this repository. They distinguish implemented behavior from production hardening that remains future work.

## 1. Tutorial / step-by-step

[Build a Production-Minded REST API with Spring Boot 4.1 and Java 21](01-building-a-production-minded-rest-api.md)

A guided walkthrough of the Product Catalog API: REST contract, request validation, PostgreSQL constraints, Flyway migrations, optimistic locking, RFC 9457 errors, Redis cache-aside, and Testcontainers integration tests.

## 2. Technical documentation

[Containerizing a Spring Boot Service with a Multi-Stage Docker Build](02-containerizing-a-spring-boot-service.md)

A line-by-line explanation of the actual Dockerfile, including build/runtime separation, non-root execution, JVM container memory settings, health checks, CI verification, and production hardening considerations.

## 3. System architecture

[Observability Architecture for a Spring Boot Product Catalog Service](03-observability-architecture.md)

An architecture overview of the running service and its telemetry flow to Prometheus and Jaeger, including bounded-cardinality metrics, OpenTelemetry tracing, and CI smoke-test verification.

## Source repository

[FatmaEmran/java-microservices-blueprints](https://github.com/FatmaEmran/java-microservices-blueprints)
