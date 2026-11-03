package com.fatmaemran.blueprints.catalog.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"))
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 64)
    private String sku;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() { }
    private Product(String sku, String name, String description, BigDecimal price) {
        this.sku = sku; this.name = name; this.description = description; this.price = price;
    }
    public static Product create(String sku, String name, String description, BigDecimal price) {
        return new Product(sku, name, description, price);
    }
    public void updateDetails(String name, String description, BigDecimal price) {
        this.name = name; this.description = description; this.price = price;
    }
    @PrePersist
    void onCreate() {
        Instant now = Instant.now(); this.createdAt = now; this.updatedAt = now;
    }
    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
