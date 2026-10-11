package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.UpdateCategoryRequest;
import com.msspoker.shopservice.entity.Category;

import java.util.UUID;

public interface UpdateCategoryValidator {
    void validate(UUID categoryId, Category category, UpdateCategoryRequest request);
}
