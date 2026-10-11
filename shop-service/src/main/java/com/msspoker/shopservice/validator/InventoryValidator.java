package com.msspoker.shopservice.validator;

import com.msspoker.shopservice.entity.InventoryItem;

import java.util.UUID;

/**
 * Business validation interface cho các thao tác với túi đồ (Rule #15 — Tier 2).
 * Mỗi rule nghiệp vụ = 1 @Component implement interface này.
 * Service inject List<InventoryValidator> và loop qua tất cả validators.
 */
public interface InventoryValidator {

    /**
     * Kiểm tra điều kiện nghiệp vụ của vật phẩm trong túi đồ.
     *
     * @param accountId  
     * @param productId 
     * @param item       
     * @throws com.msspoker.shopservice.exception.AppException 
     */
    void validate(UUID accountId, UUID productId, InventoryItem item);
}
