package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.exception.CategoryExceptions;
import com.msspoker.shopservice.repository.ProductRepository;
import com.msspoker.shopservice.validator.DeleteCategoryValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class CategoryDeletableValidator implements DeleteCategoryValidator {

    private final ProductRepository productRepository;

    @Override
    public void validate(UUID categoryId, Category category) {
        if (productRepository.existsByCategoryIdAndIsDeletedFalse(categoryId)) {
            log.warn("Category still contains active products, cannot delete: id={}", categoryId);
            throw CategoryExceptions.categoryHasProducts();
        }
    }
}
