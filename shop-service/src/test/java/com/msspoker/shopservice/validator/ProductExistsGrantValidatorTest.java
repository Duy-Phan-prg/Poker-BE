package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.GrantItemRequest;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.validator.impl.ProductExistsGrantValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductExistsGrantValidatorTest {

    private final ProductExistsGrantValidator validator = new ProductExistsGrantValidator();

    @Test
    @DisplayName("validate thành công khi product khác null")
    void validateSuccess() {
        GrantItemRequest request = new GrantItemRequest(UUID.randomUUID(), UUID.randomUUID(), "key-1");
        Product product = Product.builder().id(UUID.randomUUID()).build();

        assertThatCode(() -> validator.validate(request, product)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate ném SHOP_PRODUCT_NOT_FOUND khi product là null")
    void validateNullProductThrowsException() {
        GrantItemRequest request = new GrantItemRequest(UUID.randomUUID(), UUID.randomUUID(), "key-2");

        assertThatThrownBy(() -> validator.validate(request, null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_PRODUCT_NOT_FOUND");
                });
    }
}
