package com.msspoker.shopservice.mapper;

import com.msspoker.shopservice.dto.request.CreateCategoryRequest;
import com.msspoker.shopservice.dto.request.CreateProductRequest;
import com.msspoker.shopservice.dto.request.UpdateProductRequest;
import com.msspoker.shopservice.dto.response.CategoryResponse;
import com.msspoker.shopservice.dto.response.ProductResponse;
import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    CategoryResponse toCategoryResponse(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Category toCategory(CreateCategoryRequest request);

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "categoryCode", source = "product.category.code")
    @Mapping(target = "categoryName", source = "product.category.name")
    @Mapping(target = "name", source = "product.name")
    @Mapping(target = "description", source = "product.description")
    @Mapping(target = "tier", source = "product.tier")
    @Mapping(target = "priceVnd", source = "product.priceVnd")
    @Mapping(target = "priceVipCoin", source = "product.priceVipCoin")
    @Mapping(target = "assetUrl", source = "product.assetUrl")
    @Mapping(target = "previewUrl", source = "product.previewUrl")
    @Mapping(target = "metadata", source = "product.metadata")
    @Mapping(target = "isExclusive", source = "product.exclusive")
    @Mapping(target = "isOwned", source = "isOwned")
    ProductResponse toProductResponse(Product product, boolean isOwned);

    default ProductResponse toProductResponse(Product product) {
        return toProductResponse(product, false);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "tier", source = "request.tier")
    @Mapping(target = "priceVnd", source = "request.priceVnd")
    @Mapping(target = "assetUrl", source = "request.assetUrl")
    @Mapping(target = "isExclusive", source = "request.isExclusive")
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toProduct(CreateProductRequest request, Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "tier", source = "request.tier")
    @Mapping(target = "priceVnd", source = "request.priceVnd")
    @Mapping(target = "assetUrl", source = "request.assetUrl")
    @Mapping(target = "exclusive", source = "request.isExclusive")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProductFromRequest(@MappingTarget Product product, UpdateProductRequest request, Category category);
}
