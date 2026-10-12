package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.CreateOrderRequest;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.enums.OrderStatus;
import com.msspoker.shopservice.exception.OrderExceptions;
import com.msspoker.shopservice.exception.ProductExceptions;
import com.msspoker.shopservice.repository.InventoryItemRepository;
import com.msspoker.shopservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Validator tập trung toàn bộ các quy tắc nghiệp vụ cho Order.
 */
@Component
@RequiredArgsConstructor
public class OrderValidator {

    private final InventoryItemRepository inventoryItemRepository;
    private final OrderRepository orderRepository;

    public void validateCreateOrder(UUID accountId, CreateOrderRequest request, Product product) {
        if (product == null || product.isDeleted()) {
            throw ProductExceptions.productNotFound();
        }
        if (product.isExclusive()) {
            throw ProductExceptions.productNotPurchasable();
        }
        if (inventoryItemRepository.existsByAccountIdAndProductId(accountId, product.getId())) {
            throw ProductExceptions.productAlreadyOwned();
        }
        if (orderRepository.existsByAccountIdAndProductIdAndStatusAndExpiresAtAfter(
                accountId, product.getId(), OrderStatus.PENDING, Instant.now())) {
            throw OrderExceptions.pendingOrderAlreadyExists();
        }
    }
}
