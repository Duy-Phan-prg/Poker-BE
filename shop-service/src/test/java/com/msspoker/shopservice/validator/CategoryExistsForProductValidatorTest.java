package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.CreateProductRequest;
import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.enums.ProductTier;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.validator.impl.CategoryExistsForProductValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryExistsForProductValidatorTest {

    private final CategoryExistsForProductValidator validator = new CategoryExistsForProductValidator();

    @Test
    @DisplayName("validate thành công khi category tồn tại")
    void validateSuccess() {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(UUID.randomUUID())
                .name("Product")
                .tier(ProductTier.VIP)
                .priceVnd(1000)
                .assetUrl("url")
                .build();
        Category category = Category.builder().id(request.categoryId()).build();

        assertThatCode(() -> validator.validate(request, category)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate ném SHOP_CATEGORY_NOT_FOUND khi category là null")
    void validateNullCategoryThrows() {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(UUID.randomUUID())
                .name("Product")
                .tier(ProductTier.VIP)
                .priceVnd(1000)
                .assetUrl("url")
                .build();

        assertThatThrownBy(() -> validator.validate(request, null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_CATEGORY_NOT_FOUND");
                });
    }
}
