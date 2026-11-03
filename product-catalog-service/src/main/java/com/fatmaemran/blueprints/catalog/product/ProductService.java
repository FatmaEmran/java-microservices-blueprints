package com.fatmaemran.blueprints.catalog.product;

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

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String normalizedSku = request.sku().trim().toUpperCase(Locale.ROOT);

        if (productRepository.existsBySku(normalizedSku)) {
            throw new DuplicateSkuException(normalizedSku);
        }

        Product product = Product.create(
                normalizedSku,
                request.name().trim(),
                request.description() == null || request.description().isBlank()
                        ? null
                        : request.description().trim(),
                request.price()
        );

        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse getById(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Page<Product> list(int page, int size) {
        if (page < 0) {
            throw new InvalidPageRequestException("Page index must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException(
                    "Page size must be between 1 and %d".formatted(MAX_PAGE_SIZE)
            );
        }

        return productRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }
}
