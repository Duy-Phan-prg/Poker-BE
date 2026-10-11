package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.dto.request.CreateCategoryRequest;
import com.msspoker.shopservice.exception.CategoryExceptions;
import com.msspoker.shopservice.repository.CategoryRepository;
import com.msspoker.shopservice.validator.CreateCategoryValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CategoryCodeUniqueValidator implements CreateCategoryValidator {

    private final CategoryRepository categoryRepository;

    @Override
    public void validate(CreateCategoryRequest request) {
        if (categoryRepository.existsByCode(request.code())) {
            log.warn("Category code already exists: {}", request.code());
            throw CategoryExceptions.categoryCodeAlreadyExists();
        }
    }
}
