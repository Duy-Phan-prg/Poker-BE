package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.CreateProductRequest;
import com.msspoker.shopservice.entity.Category;

public interface CreateProductValidator {
    void validate(CreateProductRequest request, Category category);
}
