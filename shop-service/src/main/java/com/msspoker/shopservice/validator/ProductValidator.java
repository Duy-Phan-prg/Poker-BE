package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.entity.Product;

import java.util.UUID;

/**
 * Business validation interface cho các thao tác sản phẩm (Rule #15 — Tier 2).
 */
public interface ProductValidator {

    /**
     * Kiểm tra điều kiện nghiệp vụ của sản phẩm.
     *
     * @param productId  ID sản phẩm
     * @param product    Entity sản phẩm (có thể null nếu không tìm thấy)
     * @throws com.msspoker.shopservice.exception.AppException nếu vi phạm rule
     */
    void validate(UUID productId, Product product);
}
