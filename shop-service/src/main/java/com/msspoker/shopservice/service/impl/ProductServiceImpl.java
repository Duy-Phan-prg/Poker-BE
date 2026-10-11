package com.msspoker.shopservice.service.impl;

import com.msspoker.shopservice.config.RedisConfig;
import com.msspoker.shopservice.dto.request.CreateProductRequest;
import com.msspoker.shopservice.dto.request.UpdateProductRequest;
import com.msspoker.shopservice.dto.response.PageResponse;
import com.msspoker.shopservice.dto.response.ProductResponse;
import com.msspoker.shopservice.entity.Category;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.enums.ProductTier;
import com.msspoker.shopservice.mapper.ProductMapper;
import com.msspoker.shopservice.repository.CategoryRepository;
import com.msspoker.shopservice.repository.InventoryItemRepository;
import com.msspoker.shopservice.repository.ProductRepository;
import com.msspoker.shopservice.service.ProductService;
import com.msspoker.shopservice.validator.CreateProductValidator;
import com.msspoker.shopservice.validator.DeleteProductValidator;
import com.msspoker.shopservice.validator.ProductValidator;
import com.msspoker.shopservice.validator.UpdateProductValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductMapper productMapper;

    private final List<ProductValidator> productValidators;
    private final List<CreateProductValidator> createProductValidators;
    private final List<UpdateProductValidator> updateProductValidators;
    private final List<DeleteProductValidator> deleteProductValidators;

    @Autowired
    @Lazy
    private ProductServiceImpl self;


    @Override
    public PageResponse<ProductResponse> getProducts(String categoryCode, ProductTier tier, Pageable pageable, UUID accountId) {
        log.debug("Fetching available products with categoryCode={}, tier={}, accountId={}", categoryCode, tier, accountId);
        Pageable safePageable = normalizePageable(pageable);

        PageResponse<ProductResponse> catalog = (self != null)
                ? self.getProductCatalogCached(categoryCode, tier, safePageable)
                : getProductCatalogCached(categoryCode, tier, safePageable);

        if (accountId == null) {
            return catalog;
        }

        Set<UUID> ownedProductIds = inventoryItemRepository.findOwnedProductIdsByAccountId(accountId);
        if (ownedProductIds.isEmpty()) {
            return catalog;
        }

        List<ProductResponse> enriched = catalog.content().stream()
                .map(p -> p.withOwned(ownedProductIds.contains(p.id())))
                .toList();

        return new PageResponse<>(
                enriched,
                catalog.page(),
                catalog.size(),
                catalog.totalElements(),
                catalog.totalPages(),
                catalog.last()
        );
    }

    /**
     * Cache toàn bộ trang catalog sản phẩm (chưa gắn user ownership) trên Redis.
     * Key được phân bổ theo category, tier và safePageable (page, size, sort).
     * Phục vụ hàng triệu lượt đọc từ khách và người chơi mà không cần query lại database.
     */
    @Cacheable(
            value = RedisConfig.CACHE_PRODUCTS_CATALOG,
            key = "(#categoryCode != null ? #categoryCode : 'ALL') + ':' + (#tier != null ? #tier : 'ALL') + ':' + #safePageable.pageNumber + ':' + #safePageable.pageSize + ':' + #safePageable.sort"
    )
    public PageResponse<ProductResponse> getProductCatalogCached(String categoryCode, ProductTier tier, Pageable safePageable) {
        log.debug("Querying product catalog from database (cache miss): categoryCode={}, tier={}, pageable={}",
                categoryCode, tier, safePageable);
        Page<Product> productPage = productRepository.findAvailableProducts(categoryCode, tier, safePageable);
        Page<ProductResponse> responsePage = productPage.map(product -> productMapper.toProductResponse(product, false));
        return PageResponse.of(responsePage);
    }

    private static final int MAX_PAGE_NUMBER = 10_000;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "name", "tier", "priceVnd", "priceVipCoin", "createdAt", "updatedAt"
    );

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int safePage = Math.min(Math.max(0, pageable.getPageNumber()), MAX_PAGE_NUMBER);
        int safeSize = Math.min(Math.max(1, pageable.getPageSize()), 100);

        if (pageable.getSort() == null || pageable.getSort().isUnsorted()) {
            return PageRequest.of(safePage, safeSize);
        }


        List<Sort.Order> validOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                validOrders.add(order);
            }
        }

        Sort safeSort = validOrders.isEmpty() ? Sort.unsorted() : Sort.by(validOrders);
        return PageRequest.of(safePage, safeSize, safeSort);
    }


    @Override
    public ProductResponse getProductDetail(UUID id, UUID accountId) {
        log.debug("Fetching product detail for id={}, accountId={}", id, accountId);
        Product product = (self != null) ? self.getProductEntityCached(id) : getProductEntityCached(id);

        boolean isOwned = accountId != null && inventoryItemRepository.existsByAccountIdAndProductId(accountId, id);

        return productMapper.toProductResponse(product, isOwned);
    }

    /**
     * Cache chi tiết sản phẩm trên Redis theo product id để phục vụ lượng truy cập lớn.
     */
    @Cacheable(value = RedisConfig.CACHE_PRODUCTS, key = "#id")
    public Product getProductEntityCached(UUID id) {
        log.debug("Querying product from database (cache miss): id={}", id);
        Product product = productRepository.findByIdAndIsDeletedFalse(id).orElse(null);
        productValidators.forEach(validator -> validator.validate(id, product));
        return product;
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_CATALOG, allEntries = true)
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating new product with name={}, categoryId={}", request.name(), request.categoryId());
        Category category = categoryRepository.findById(request.categoryId()).orElse(null);
        createProductValidators.forEach(validator -> validator.validate(request, category));

        Product product = productMapper.toProduct(request, category);
        Product saved = productRepository.save(product);
        log.info("Created product id={} successfully", saved.getId());
        return productMapper.toProductResponse(saved, false);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS, key = "#id"),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_CATALOG, allEntries = true)
    })
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        log.info("Updating product id={} with name={}", id, request.name());
        Product product = productRepository.findByIdAndIsDeletedFalse(id).orElse(null);
        Category category = categoryRepository.findById(request.categoryId()).orElse(null);

        updateProductValidators.forEach(validator -> validator.validate(id, product, category, request));

        productMapper.updateProductFromRequest(product, request, category);
        log.info("Updated product id={} successfully", id);
        return productMapper.toProductResponse(product, false);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS, key = "#id"),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS_CATALOG, allEntries = true)
    })
    public void deleteProduct(UUID id) {
        log.info("Soft-deleting product id={}", id);
        Product product = productRepository.findByIdAndIsDeletedFalse(id).orElse(null);
        deleteProductValidators.forEach(validator -> validator.validate(id, product));

        product.setDeleted(true);
        log.info("Soft-deleted product id={}", id);
    }
}
