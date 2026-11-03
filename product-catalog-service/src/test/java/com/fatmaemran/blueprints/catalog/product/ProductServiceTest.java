package com.fatmaemran.blueprints.catalog.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createNormalizesSkuAndTrimsOptionalText() {
        when(productRepository.existsBySku("SKU-123")).thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateProductRequest request = new CreateProductRequest(
                " sku-123 ",
                "  Mechanical Keyboard  ",
                "  Compact keyboard  ",
                new BigDecimal("129.99")
        );

        ProductResponse response = productService.create(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        assertThat(captor.getValue().getSku()).isEqualTo("SKU-123");
        assertThat(response.name()).isEqualTo("Mechanical Keyboard");
        assertThat(response.description()).isEqualTo("Compact keyboard");
        assertThat(response.price()).isEqualByComparingTo("129.99");
    }

    @Test
    void createRejectsAnExistingSku() {
        when(productRepository.existsBySku("SKU-123")).thenReturn(true);

        CreateProductRequest request = new CreateProductRequest(
                "sku-123", "Keyboard", null, new BigDecimal("129.99")
        );

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateSkuException.class);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getByIdRejectsUnknownProduct() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void listRejectsPageSizesAboveTheLimit() {
        assertThatThrownBy(() -> productService.list(0, 101))
                .isInstanceOf(InvalidPageRequestException.class);
    }
}
