package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.dto.request.CreateOrderRequest;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.enums.OrderStatus;
import com.msspoker.shopservice.exception.OrderExceptions;
import com.msspoker.shopservice.repository.OrderRepository;
import com.msspoker.shopservice.validator.CreateOrderValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Order(30)
@RequiredArgsConstructor
public class NoPendingOrderValidator implements CreateOrderValidator {

    private final OrderRepository orderRepository;

    @Override
    public void validate(UUID accountId, CreateOrderRequest request, Product product) {
        if (product != null) {
            boolean hasPendingOrder = orderRepository.existsByAccountIdAndProductIdAndStatusAndExpiresAtAfter(
                    accountId, product.getId(), OrderStatus.PENDING, Instant.now()
            );
            if (hasPendingOrder) {
                throw OrderExceptions.pendingOrderAlreadyExists();
            }
        }
    }
}
