package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.entity.Product;

import java.util.UUID;

public interface DeleteProductValidator {
    void validate(UUID productId, Product product);
}
