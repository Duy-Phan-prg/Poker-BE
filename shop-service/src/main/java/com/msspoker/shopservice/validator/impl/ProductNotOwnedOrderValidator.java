package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.dto.request.CreateOrderRequest;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.ProductExceptions;
import com.msspoker.shopservice.repository.InventoryItemRepository;
import com.msspoker.shopservice.validator.CreateOrderValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Order(20)
@RequiredArgsConstructor
public class ProductNotOwnedOrderValidator implements CreateOrderValidator {

    private final InventoryItemRepository inventoryItemRepository;

    @Override
    public void validate(UUID accountId, CreateOrderRequest request, Product product) {
        if (product != null && inventoryItemRepository.existsByAccountIdAndProductId(accountId, product.getId())) {
            throw ProductExceptions.productAlreadyOwned();
        }
    }
}
