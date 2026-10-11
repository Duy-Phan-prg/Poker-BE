package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.dto.request.CreateOrderRequest;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.ProductExceptions;
import com.msspoker.shopservice.validator.CreateOrderValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Order(10)
public class ProductPurchasableOrderValidator implements CreateOrderValidator {

    @Override
    public void validate(UUID accountId, CreateOrderRequest request, Product product) {
        if (product == null || product.isDeleted()) {
            throw ProductExceptions.productNotFound();
        }
        if (product.isExclusive()) {
            throw ProductExceptions.productNotPurchasable();
        }
    }
}
