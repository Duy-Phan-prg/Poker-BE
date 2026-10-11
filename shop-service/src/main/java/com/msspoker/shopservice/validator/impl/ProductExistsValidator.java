package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.ProductExceptions;
import com.msspoker.shopservice.validator.ProductValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kiểm tra sản phẩm phải tồn tại và chưa bị soft-delete (Rule #15 — Tier 2).
 * Order(1) = chạy đầu tiên trong chuỗi ProductValidator.
 */
@Slf4j
@Component
@Order(1)
public class ProductExistsValidator implements ProductValidator {

    @Override
    public void validate(UUID productId, Product product) {
        if (product == null) {
            log.warn("Product not found or soft-deleted: productId={}", productId);
            throw ProductExceptions.productNotFound();
        }
    }
}
