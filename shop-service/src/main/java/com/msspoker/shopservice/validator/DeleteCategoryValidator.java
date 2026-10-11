package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.entity.Category;

import java.util.UUID;

public interface DeleteCategoryValidator {
    void validate(UUID categoryId, Category category);
}
