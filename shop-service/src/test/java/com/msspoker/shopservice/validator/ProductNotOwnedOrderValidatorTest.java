package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.CreateOrderRequest;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.repository.InventoryItemRepository;
import com.msspoker.shopservice.validator.impl.ProductNotOwnedOrderValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductNotOwnedOrderValidatorTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private ProductNotOwnedOrderValidator validator;

    private UUID accountId;
    private Product product;
    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        product = Product.builder().id(UUID.randomUUID()).build();
        request = new CreateOrderRequest(product.getId());
    }

    @Test
    @DisplayName("validate - Chưa sở hữu sản phẩm thì pass")
    void validate_NotOwned_Success() {
        when(inventoryItemRepository.existsByAccountIdAndProductId(accountId, product.getId()))
                .thenReturn(false);

        assertThatCode(() -> validator.validate(accountId, request, product))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate - Đã sở hữu sản phẩm thì ném SHOP_PRODUCT_ALREADY_OWNED")
    void validate_AlreadyOwned_ThrowsException() {
        when(inventoryItemRepository.existsByAccountIdAndProductId(accountId, product.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> validator.validate(accountId, request, product))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", "SHOP_PRODUCT_ALREADY_OWNED")
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }
}
