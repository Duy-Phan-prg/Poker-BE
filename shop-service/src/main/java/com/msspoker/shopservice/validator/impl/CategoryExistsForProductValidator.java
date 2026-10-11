package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.dto.request.CreateProductRequest;
import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.exception.CategoryExceptions;
import com.msspoker.shopservice.validator.CreateProductValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
public class CategoryExistsForProductValidator implements CreateProductValidator {

    @Override
    public void validate(CreateProductRequest request, Category category) {
        if (category == null) {
            log.warn("Category not found for product creation: categoryId={}", request.categoryId());
            throw CategoryExceptions.categoryNotFound();
        }
    }
}
