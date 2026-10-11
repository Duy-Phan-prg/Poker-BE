package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.UpdateProductRequest;
import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.entity.Product;

import java.util.UUID;

public interface UpdateProductValidator {
    void validate(UUID productId, Product product, Category category, UpdateProductRequest request);
}
