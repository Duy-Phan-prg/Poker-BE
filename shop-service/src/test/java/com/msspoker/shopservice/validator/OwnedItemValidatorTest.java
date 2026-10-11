package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.entity.InventoryItem;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.validator.impl.OwnedItemValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnedItemValidatorTest {

    private final OwnedItemValidator validator = new OwnedItemValidator();

    @Test
    @DisplayName("validate thành công khi item khác null")
    void validateSuccess() {
        InventoryItem item = InventoryItem.builder().id(UUID.randomUUID()).build();
        assertThatCode(() -> validator.validate(UUID.randomUUID(), UUID.randomUUID(), item))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate ném SHOP_ITEM_NOT_OWNED khi item là null")
    void validateNullItemThrowsException() {
        UUID accountId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        assertThatThrownBy(() -> validator.validate(accountId, productId, null))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ex = (AppException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("SHOP_ITEM_NOT_OWNED");
                });
    }
}
