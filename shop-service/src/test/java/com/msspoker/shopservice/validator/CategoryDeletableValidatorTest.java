package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.repository.ProductRepository;
import com.msspoker.shopservice.validator.impl.CategoryDeletableValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryDeletableValidatorTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryDeletableValidator validator;

    @Test
    @DisplayName("validate thành công khi category không có sản phẩm")
    void validateSuccess() {
        UUID categoryId = UUID.randomUUID();
        when(productRepository.existsByCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(false);

        assertThatCode(() -> validator.validate(categoryId, Category.builder().id(categoryId).build()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate ném SHOP_CATEGORY_HAS_PRODUCTS khi còn sản phẩm")
    void validateHasProductsThrows() {
        UUID categoryId = UUID.randomUUID();
        when(productRepository.existsByCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(categoryId, Category.builder().id(categoryId).build()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_CATEGORY_HAS_PRODUCTS");
                });
    }
}
