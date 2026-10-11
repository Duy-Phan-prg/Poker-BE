package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.UpdateCategoryRequest;
import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.validator.impl.CategoryExistsDeleteValidator;
import com.msspoker.shopservice.validator.impl.CategoryExistsUpdateValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryExistsValidatorTest {

    private final CategoryExistsUpdateValidator updateValidator = new CategoryExistsUpdateValidator();
    private final CategoryExistsDeleteValidator deleteValidator = new CategoryExistsDeleteValidator();

    @Test
    @DisplayName("updateValidator thành công khi category tồn tại")
    void updateValidatorSuccess() {
        Category category = Category.builder().id(UUID.randomUUID()).build();
        assertThatCode(() -> updateValidator.validate(category.getId(), category, new UpdateCategoryRequest("FRAME", "Name")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("updateValidator ném SHOP_CATEGORY_NOT_FOUND khi category là null")
    void updateValidatorNullThrows() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> updateValidator.validate(id, null, new UpdateCategoryRequest("FRAME", "Name")))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_CATEGORY_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("deleteValidator thành công khi category tồn tại")
    void deleteValidatorSuccess() {
        Category category = Category.builder().id(UUID.randomUUID()).build();
        assertThatCode(() -> deleteValidator.validate(category.getId(), category))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteValidator ném SHOP_CATEGORY_NOT_FOUND khi category là null")
    void deleteValidatorNullThrows() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> deleteValidator.validate(id, null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_CATEGORY_NOT_FOUND");
                });
    }
}
