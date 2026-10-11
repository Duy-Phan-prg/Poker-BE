package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.dto.request.GrantItemRequest;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.exception.ProductExceptions;
import com.msspoker.shopservice.validator.GrantValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra sản phẩm được trao thưởng phải tồn tại trong catalog (Rule #15 — Tier 2).
 * Order(1) = chạy đầu tiên trong chuỗi GrantValidator.
 */
@Slf4j
@Component
@Order(1)
public class ProductExistsGrantValidator implements GrantValidator {

    @Override
    public void validate(GrantItemRequest request, Product product) {
        if (product == null) {
            log.warn("Product not found or soft-deleted for grant request: productId={}", request.productId());
            throw ProductExceptions.productNotFound();
        }
    }
}
