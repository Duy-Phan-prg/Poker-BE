package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.CreateOrderRequest;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.validator.impl.ProductPurchasableOrderValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductPurchasableOrderValidatorTest {

    private ProductPurchasableOrderValidator validator;
    private UUID accountId;
    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        validator = new ProductPurchasableOrderValidator();
        accountId = UUID.randomUUID();
        request = new CreateOrderRequest(UUID.randomUUID());
    }

    @Test
    @DisplayName("validate - Sản phẩm bình thường, không độc quyền thì pass")
    void validate_PurchasableProduct_Success() {
        Product product = Product.builder()
                .id(request.productId())
                .isExclusive(false)
                .isDeleted(false)
                .build();

        assertThatCode(() -> validator.validate(accountId, request, product))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate - Sản phẩm null ném SHOP_PRODUCT_NOT_FOUND")
    void validate_NullProduct_ThrowsException() {
        assertThatThrownBy(() -> validator.validate(accountId, request, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", "SHOP_PRODUCT_NOT_FOUND")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("validate - Sản phẩm đã bị xóa ném SHOP_PRODUCT_NOT_FOUND")
    void validate_DeletedProduct_ThrowsException() {
        Product product = Product.builder()
                .id(request.productId())
                .isExclusive(false)
                .isDeleted(true)
                .build();

        assertThatThrownBy(() -> validator.validate(accountId, request, product))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", "SHOP_PRODUCT_NOT_FOUND")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("validate - Sản phẩm là phần thưởng độc quyền ném SHOP_PRODUCT_NOT_PURCHASABLE")
    void validate_ExclusiveProduct_ThrowsException() {
        Product product = Product.builder()
                .id(request.productId())
                .isExclusive(true)
                .isDeleted(false)
                .build();

        assertThatThrownBy(() -> validator.validate(accountId, request, product))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", "SHOP_PRODUCT_NOT_PURCHASABLE")
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }
}
