package com.fatmaemran.blueprints.catalog.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void createsAndReadsAProductUsingPostgreSql() {
        CreateProductRequest request = new CreateProductRequest(
                "integration-keyboard",
                "Integration Keyboard",
                "Created through the HTTP API",
                new BigDecimal("89.50")
        );

        ResponseEntity<ProductResponse> created = restTemplate.postForEntity(
                "/api/v1/products",
                request,
                ProductResponse.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().sku()).isEqualTo("INTEGRATION-KEYBOARD");

        URI location = URI.create("http://localhost:" + port
                + created.getHeaders().getLocation().getPath());

        ResponseEntity<ProductResponse> fetched =
                restTemplate.getForEntity(location, ProductResponse.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().id()).isEqualTo(created.getBody().id());
        assertThat(fetched.getBody().price()).isEqualByComparingTo("89.50");
    }

    @Test
    void rejectsDuplicateSkuWithConflict() {
        CreateProductRequest request = new CreateProductRequest(
                "duplicate-key",
                "First Product",
                null,
                new BigDecimal("10.00")
        );

        ResponseEntity<ProductResponse> first = restTemplate.postForEntity(
                "/api/v1/products", request, ProductResponse.class
        );
        ResponseEntity<String> duplicate = restTemplate.postForEntity(
                "/api/v1/products", request, String.class
        );

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
