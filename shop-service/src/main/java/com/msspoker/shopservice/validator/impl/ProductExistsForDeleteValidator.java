package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.ProductExceptions;
import com.msspoker.shopservice.validator.DeleteProductValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class ProductExistsForDeleteValidator implements DeleteProductValidator {

    @Override
    public void validate(UUID productId, Product product) {
        if (product == null) {
            log.warn("Product not found for delete: id={}", productId);
            throw ProductExceptions.productNotFound();
        }
    }
}
