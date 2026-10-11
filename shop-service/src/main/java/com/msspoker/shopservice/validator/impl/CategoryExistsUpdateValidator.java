package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.dto.request.UpdateCategoryRequest;
import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.exception.CategoryExceptions;
import com.msspoker.shopservice.validator.UpdateCategoryValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class CategoryExistsUpdateValidator implements UpdateCategoryValidator {

    @Override
    public void validate(UUID categoryId, Category category, UpdateCategoryRequest request) {
        if (category == null) {
            log.warn("Category not found for update: id={}", categoryId);
            throw CategoryExceptions.categoryNotFound();
        }
    }
}
