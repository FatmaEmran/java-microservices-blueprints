# Containerizing a Spring Boot Service with a Multi-Stage Docker Build

**Sample type:** Technical documentation  
**Implementation:** [product-catalog-service/Dockerfile](../../product-catalog-service/Dockerfile)  
**Runtime:** Java 21 on Eclipse Temurin JRE Alpine

A container image is part of a service's runtime design. The build environment needs compilers and build tools; the runtime environment usually does not. A multi-stage Docker build keeps those concerns separate and reduces the software shipped with the application.

This document explains the actual Dockerfile used by the Product Catalog service and the trade-offs behind it.

## The Dockerfile

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build --chown=app:app /workspace/target/product-catalog-service-0.1.0-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --spider http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
```

## 1. Separate build and runtime stages

The first stage uses a Maven image with a Java 21 JDK. It compiles the project and packages the executable Spring Boot JAR.

The second stage starts from a Java 21 JRE image. It copies only the packaged artifact from the build stage. Maven, source files, and the JDK are not copied into the final runtime stage.

This reduces the runtime image's toolset and keeps the build environment out of the deployed container. It is not, by itself, a complete supply-chain security strategy.

## 2. Run as a non-root user

The runtime stage creates a dedicated `app` group and user, assigns ownership of the application directory, and switches to that user before starting Java:

```dockerfile
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/product-catalog-service-0.1.0-SNAPSHOT.jar app.jar
USER app
```

The application does not need root privileges to listen on port 8080 or read its packaged JAR. Running as a non-root user limits the impact of some classes of container compromise.

## 3. Respect container memory limits

The JVM is started with:

```bash
-XX:MaxRAMPercentage=75.0
```

This tells the JVM to size its memory ceiling as a percentage of the memory available to the container, rather than assuming it can use the host's entire memory. The chosen percentage is a starting point, not a universal tuning value. Heap, metaspace, direct buffers, thread stacks, and native allocations all contribute to the process's total memory footprint.

Load testing and container-level memory monitoring are still required before choosing production limits.

## 4. Add a container health check

The image declares a Docker health check against Spring Boot Actuator:

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --spider http://127.0.0.1:8080/actuator/health || exit 1
```

The start period gives the application time to initialize before failures count toward the retry threshold. The check uses the application's health endpoint, so the endpoint must remain available inside the container.

A Docker health check is not a replacement for orchestrator readiness and liveness policy. In a production deployment, management endpoints should be appropriately isolated and secured.

## 5. Understand where tests run

The Docker build command uses `-DskipTests package`. This means the image build itself does not execute the test suite.

The repository's GitHub Actions workflow runs Maven verification separately, including integration tests, before building the Docker image. Keeping verification and packaging as distinct CI steps makes the pipeline's responsibilities explicit.

For local verification:

```bash
mvn -f product-catalog-service/pom.xml verify
```

For a local image and Compose stack:

```bash
docker compose -f product-catalog-service/compose.yaml up --build -d
```

## Production hardening checklist

The current Dockerfile is a useful baseline, not a final hardened supply-chain configuration. Before production use:

- Pin base images by digest rather than relying only on mutable tags.
- Scan the base image and application dependencies.
- Generate and retain a software bill of materials (SBOM).
- Sign and verify released images.
- Run with a read-only root filesystem where compatible, and provide explicit writable temporary storage if required.
- Set CPU and memory requests/limits in the deployment platform.
- Configure secrets outside the image and avoid embedding credentials in build arguments or layers.
- Define shutdown grace periods and verify graceful termination under load.
- Keep the management interface private and expose only the health information required by the platform.

## Summary

The multi-stage build separates Maven/JDK tooling from the JRE runtime. A dedicated non-root user reduces privileges, the JVM respects container memory constraints, and Actuator provides a basic health signal. CI runs verification independently from image packaging.

**Source:** [Product Catalog Dockerfile](https://github.com/FatmaEmran/java-microservices-blueprints/blob/main/product-catalog-service/Dockerfile)
