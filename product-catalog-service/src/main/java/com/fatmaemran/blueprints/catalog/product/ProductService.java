package com.fatmaemran.blueprints.catalog.product;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private static final int MAX_PAGE_SIZE = 100;
    private final ProductRepository productRepository;
    public ProductService(ProductRepository productRepository) { this.productRepository = productRepository; }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsBySku(sku)) throw new DuplicateSkuException(sku);
        Product product = Product.create(sku, request.name().trim(),
                normalizeDescription(request.description()), request.price());
        return ProductResponse.from(productRepository.save(product));
    }

    @Cacheable(cacheNames = "product-by-id", key = "#id")
    public ProductResponse getById(UUID id) {
        return productRepository.findById(id).map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    @CacheEvict(cacheNames = "product-by-id", key = "#id")
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.updateDetails(request.name().trim(), normalizeDescription(request.description()), request.price());
        return ProductResponse.from(productRepository.saveAndFlush(product));
    }

    @Transactional
    @CacheEvict(cacheNames = "product-by-id", key = "#id")
    public void delete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.delete(product);
    }

    public Page<Product> list(int page, int size) {
        if (page < 0) throw new InvalidPageRequestException("Page index must be zero or greater");
        if (size < 1 || size > MAX_PAGE_SIZE)
            throw new InvalidPageRequestException("Page size must be between 1 and %d".formatted(MAX_PAGE_SIZE));
        return productRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
