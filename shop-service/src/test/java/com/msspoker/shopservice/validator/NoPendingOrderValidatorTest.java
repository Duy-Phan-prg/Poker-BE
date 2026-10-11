package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.CreateOrderRequest;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.enums.OrderStatus;
import com.msspoker.shopservice.exception.AppException;
import com.msspoker.shopservice.repository.OrderRepository;
import com.msspoker.shopservice.validator.impl.NoPendingOrderValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoPendingOrderValidatorTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private NoPendingOrderValidator validator;

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
    @DisplayName("validate - Không có đơn hàng PENDING nào đang tồn tại thì pass")
    void validate_NoPendingOrder_Success() {
        when(orderRepository.existsByAccountIdAndProductIdAndStatusAndExpiresAtAfter(
                eq(accountId), eq(product.getId()), eq(OrderStatus.PENDING), any(Instant.class)
        )).thenReturn(false);

        assertThatCode(() -> validator.validate(accountId, request, product))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate - Đã có đơn hàng PENDING chưa hết hạn thì ném SHOP_ORDER_PENDING_EXISTS")
    void validate_HasPendingOrder_ThrowsException() {
        when(orderRepository.existsByAccountIdAndProductIdAndStatusAndExpiresAtAfter(
                eq(accountId), eq(product.getId()), eq(OrderStatus.PENDING), any(Instant.class)
        )).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(accountId, request, product))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", "SHOP_ORDER_PENDING_EXISTS")
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }
}
