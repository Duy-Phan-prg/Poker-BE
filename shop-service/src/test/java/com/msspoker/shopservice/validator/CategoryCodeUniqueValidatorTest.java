package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.CreateCategoryRequest;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.repository.CategoryRepository;
import com.msspoker.shopservice.validator.impl.CategoryCodeUniqueValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryCodeUniqueValidatorTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryCodeUniqueValidator validator;

    @Test
    @DisplayName("validate thành công khi category code chưa tồn tại")
    void validateSuccess() {
        CreateCategoryRequest request = new CreateCategoryRequest("CARD_BACK", "Mặt lưng");
        when(categoryRepository.existsByCode("CARD_BACK")).thenReturn(false);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate ném SHOP_CATEGORY_CODE_EXISTS khi code đã tồn tại")
    void validateDuplicateThrows() {
        CreateCategoryRequest request = new CreateCategoryRequest("CARD_BACK", "Mặt lưng");
        when(categoryRepository.existsByCode("CARD_BACK")).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_CATEGORY_CODE_EXISTS");
                });
    }
}
