package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.UpdateProductRequest;
import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.enums.ProductTier;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.validator.impl.ProductExistsForUpdateValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductExistsForUpdateValidatorTest {

    private final ProductExistsForUpdateValidator validator = new ProductExistsForUpdateValidator();

    @Test
    @DisplayName("validate thành công khi product và category đều tồn tại")
    void validateSuccess() {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.builder().id(productId).build();
        Category category = Category.builder().id(categoryId).build();
        UpdateProductRequest request = UpdateProductRequest.builder().categoryId(categoryId).build();

        assertThatCode(() -> validator.validate(productId, product, category, request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate ném SHOP_PRODUCT_NOT_FOUND khi product là null")
    void validateNullProductThrows() {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).build();
        UpdateProductRequest request = UpdateProductRequest.builder().categoryId(categoryId).build();

        assertThatThrownBy(() -> validator.validate(productId, null, category, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_PRODUCT_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("validate ném SHOP_CATEGORY_NOT_FOUND khi category là null")
    void validateNullCategoryThrows() {
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.builder().id(productId).build();
        UpdateProductRequest request = UpdateProductRequest.builder().categoryId(categoryId).build();

        assertThatThrownBy(() -> validator.validate(productId, product, null, request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_CATEGORY_NOT_FOUND");
                });
    }
}
