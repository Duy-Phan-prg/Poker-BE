package com.msspoker.shopservice.validator.impl;

import com.msspoker.shopservice.entity.InventoryItem;
import com.msspoker.shopservice.exception.InventoryExceptions;
import com.msspoker.shopservice.validator.InventoryValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kiểm tra người chơi phải sở hữu vật phẩm trong túi đồ (Rule #15 — Tier 2).
 * Order(1) = chạy đầu tiên trong chuỗi InventoryValidator.
 */
@Slf4j
@Component
@Order(1)
public class OwnedItemValidator implements InventoryValidator {

    @Override
    public void validate(UUID accountId, UUID productId, InventoryItem item) {
        if (item == null) {
            log.warn("Item not owned in inventory: accountId={}, productId={}", accountId, productId);
            throw InventoryExceptions.itemNotOwned();
        }
    }
}
