package com.msspoker.shopservice.mapper;

import com.msspoker.shopservice.dto.request.GrantItemRequest;
import com.msspoker.shopservice.dto.response.GrantItemResponse;
import com.msspoker.shopservice.dto.response.InventoryItemResponse;
import com.msspoker.shopservice.dto.response.OwnershipResponse;
import com.msspoker.shopservice.entity.InventoryItem;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.enums.ItemSource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "productId", source = "item.product.id")
    @Mapping(target = "productName", source = "item.product.name")
    @Mapping(target = "categoryCode", source = "item.product.category.code")
    @Mapping(target = "categoryName", source = "item.product.category.name")
    @Mapping(target = "assetUrl", source = "item.product.assetUrl")
    @Mapping(target = "tier", source = "item.product.tier")
    @Mapping(target = "source", source = "item.source")
    @Mapping(target = "isEquipped", source = "item.equipped")
    @Mapping(target = "acquiredAt", source = "item.acquiredAt")
    InventoryItemResponse toInventoryItemResponse(InventoryItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", source = "product")
    @Mapping(target = "accountId", source = "request.accountId")
    @Mapping(target = "grantKey", source = "request.idempotencyKey")
    @Mapping(target = "source", source = "source")
    @Mapping(target = "isEquipped", constant = "false")
    @Mapping(target = "acquiredAt", ignore = true)
    InventoryItem toInventoryItem(GrantItemRequest request, Product product, ItemSource source);

    default GrantItemResponse toGrantItemResponse(boolean granted, boolean alreadyOwned, InventoryItemResponse item) {
        return new GrantItemResponse(granted, alreadyOwned, item);
    }

    default OwnershipResponse toOwnershipResponse(boolean owned) {
        return new OwnershipResponse(owned);
    }
}
