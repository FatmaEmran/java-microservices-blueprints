package com.fatmaemran.blueprints.catalog.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.Duration;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    @Container static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private CacheManager cacheManager;
    @LocalServerPort private int port;

    @Test
    void createsAndReadsAProductUsingPostgreSqlAndRedis() {
        CreateProductRequest request = new CreateProductRequest("integration-keyboard", "Integration Keyboard",
                "Created through the HTTP API", new BigDecimal("89.50"));
        ResponseEntity<ProductResponse> created = restTemplate.postForEntity("/api/v1/products", request, ProductResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().sku()).isEqualTo("INTEGRATION-KEYBOARD");
        URI location = URI.create("http://localhost:" + port + created.getHeaders().getLocation().getPath());
        ResponseEntity<ProductResponse> fetched = restTemplate.getForEntity(location, ProductResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().id()).isEqualTo(created.getBody().id());
        assertThat(fetched.getBody().price()).isEqualByComparingTo("89.50");

        Cache cache = cacheManager.getCache("product-by-id");
        assertThat(cache).isNotNull();
        ProductResponse cached = cache.get(created.getBody().id(), ProductResponse.class);
        assertThat(cached).isNotNull();
        assertThat(cached.id()).isEqualTo(created.getBody().id());
        assertThat(cached.sku()).isEqualTo(created.getBody().sku());
        assertThat(cached.name()).isEqualTo(created.getBody().name());
        assertThat(cached.price()).isEqualByComparingTo(created.getBody().price());
        assertThat(Duration.between(created.getBody().createdAt(), cached.createdAt()).abs())
                .isLessThanOrEqualTo(Duration.of(1, ChronoUnit.MICROS));
    }

    @Test
    void updateEvictsCachedProductAndNextReadReturnsFreshData() {
        ProductResponse created = createProduct("cache-update-key", "Before update");
        restTemplate.getForEntity("/api/v1/products/" + created.id(), ProductResponse.class);
        Cache cache = cacheManager.getCache("product-by-id");
        assertThat(cache.get(created.id())).isNotNull();
        UpdateProductRequest update = new UpdateProductRequest("After update", "Updated through the API", new BigDecimal("25.00"));
        ResponseEntity<ProductResponse> updated = restTemplate.exchange("/api/v1/products/" + created.id(),
                HttpMethod.PUT, new HttpEntity<>(update), ProductResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isNotNull();
        assertThat(updated.getBody().name()).isEqualTo("After update");
        assertThat(cache.get(created.id())).isNull();
        ProductResponse fetched = restTemplate.getForObject("/api/v1/products/" + created.id(), ProductResponse.class);
        assertThat(fetched).isNotNull();
        assertThat(fetched.name()).isEqualTo("After update");
        assertThat(fetched.price()).isEqualByComparingTo("25.00");
    }

    @Test
    void deleteEvictsCachedProductAndSubsequentReadReturnsNotFound() {
        ProductResponse created = createProduct("cache-delete-key", "Delete me");
        restTemplate.getForEntity("/api/v1/products/" + created.id(), ProductResponse.class);
        Cache cache = cacheManager.getCache("product-by-id");
        assertThat(cache.get(created.id())).isNotNull();
        ResponseEntity<Void> deleted = restTemplate.exchange("/api/v1/products/" + created.id(), HttpMethod.DELETE, null, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(cache.get(created.id())).isNull();
        ResponseEntity<String> missing = restTemplate.getForEntity("/api/v1/products/" + created.id(), String.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsDuplicateSkuWithConflict() {
        CreateProductRequest request = new CreateProductRequest("duplicate-key", "First Product", null, new BigDecimal("10.00"));
        ResponseEntity<ProductResponse> first = restTemplate.postForEntity("/api/v1/products", request, ProductResponse.class);
        ResponseEntity<String> duplicate = restTemplate.postForEntity("/api/v1/products", request, String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }


    @Test
    void exposesPrometheusMetricsThroughActuator() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }


    @Test
    void recordsProductCreationObservationAsPrometheusMetric() {
        createProduct("observed-create-key", "Observed Product");
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("catalog_product_create_seconds_count");
    }

    @Test
    void developmentOnlySlowEndpointIsNotAvailableInDefaultProfile() {
        ResponseEntity<String> response = restTemplate.getForEntity("/internal/dev/slow", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ProductResponse createProduct(String sku, String name) {
        ResponseEntity<ProductResponse> response = restTemplate.postForEntity("/api/v1/products",
                new CreateProductRequest(sku, name, null, new BigDecimal("12.00")), ProductResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
}
