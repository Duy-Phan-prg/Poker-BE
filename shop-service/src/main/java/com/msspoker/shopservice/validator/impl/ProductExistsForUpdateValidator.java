package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.dto.request.UpdateProductRequest;
import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.CategoryExceptions;
import com.msspoker.shopservice.exception.ProductExceptions;
import com.msspoker.shopservice.validator.UpdateProductValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class ProductExistsForUpdateValidator implements UpdateProductValidator {

    @Override
    public void validate(UUID productId, Product product, Category category, UpdateProductRequest request) {
        if (product == null) {
            log.warn("Product not found for update: id={}", productId);
            throw ProductExceptions.productNotFound();
        }
        if (category == null) {
            log.warn("Target category not found for product update: categoryId={}", request.categoryId());
            throw CategoryExceptions.categoryNotFound();
        }
    }
}
