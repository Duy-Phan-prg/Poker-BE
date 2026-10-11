package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.dto.request.GrantItemRequest;
import com.msspoker.shopservice.entity.Product;

/**
 * Business validation interface cho thao tác cấp phát phần thưởng (Rule #15 — Tier 2).
 */
public interface GrantValidator {

    /**
     * Kiểm tra điều kiện nghiệp vụ trước khi cấp phát vật phẩm.
     *
     * @param request  Yêu cầu grant
     * @param product  Entity sản phẩm tương ứng (có thể null nếu không tìm thấy)
     * @throws com.msspoker.shopservice.exception.AppException nếu vi phạm rule
     */
    void validate(GrantItemRequest request, Product product);
}
