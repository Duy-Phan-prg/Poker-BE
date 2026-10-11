package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.validator.impl.ProductExistsForDeleteValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductExistsForDeleteValidatorTest {

    private final ProductExistsForDeleteValidator validator = new ProductExistsForDeleteValidator();

    @Test
    @DisplayName("validate thành công khi product tồn tại")
    void validateSuccess() {
        Product product = Product.builder().id(UUID.randomUUID()).build();
        assertThatCode(() -> validator.validate(product.getId(), product)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate ném SHOP_PRODUCT_NOT_FOUND khi product là null")
    void validateNullThrows() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> validator.validate(id, null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_PRODUCT_NOT_FOUND");
                });
    }
}
