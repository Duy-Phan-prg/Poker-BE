package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.CreateOrderRequest;
import com.msspoker.shopservice.entity.Product;

import java.util.UUID;

public interface CreateOrderValidator {
    void validate(UUID accountId, CreateOrderRequest request, Product product);
}
