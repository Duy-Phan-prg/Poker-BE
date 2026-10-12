package com.msspoker.shopservice.controller;

import com.msspoker.shopservice.dto.request.CreateProductRequest;
import com.msspoker.shopservice.dto.request.UpdateProductRequest;
import com.msspoker.shopservice.dto.response.ApiResponse;
import com.msspoker.shopservice.dto.response.InventoryItemResponse;
import com.msspoker.shopservice.dto.response.PageResponse;
import com.msspoker.shopservice.dto.response.ProductResponse;
import com.msspoker.shopservice.enums.ProductTier;
import com.msspoker.shopservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
@Tag(name = "Public Shop Product APIs", description = "Các API duyệt và quản lý sản phẩm trong cửa hàng")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Xem danh sách sản phẩm", description = "Lọc theo category, tier, phân trang và kiểm tra cờ sở hữu nếu có X-User-Id")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @Parameter(description = "Mã danh mục cần lọc (tùy chọn)")
            @RequestParam(required = false) String category,
            @Parameter(description = "Hạng sản phẩm cần lọc ")
            @RequestParam(required = false) ProductTier tier,
            @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable,
            @Parameter(name = "X-User-Id", description = "UUID tài khoản (tùy chọn để tính cờ isOwned)", in = ParameterIn.HEADER)
            @RequestHeader(value = "X-User-Id", required = false) UUID accountId

    ) {
        PageResponse<ProductResponse> response = productService.getProducts(category, tier, pageable, accountId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết sản phẩm", description = "Lấy thông tin chi tiết của một sản phẩm theo ID (hỗ trợ Redis cache)")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductDetail(
            @Parameter(description = "ID của sản phẩm")
            @PathVariable UUID id,
            @Parameter(name = "X-User-Id", description = "UUID tài khoản (tùy chọn để tính cờ isOwned)", in = ParameterIn.HEADER)
            @RequestHeader(value = "X-User-Id", required = false) UUID accountId
    ) {
        ProductResponse response = productService.getProductDetail(id, accountId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sản phẩm thành công", response));
    }

    @PostMapping
    @Operation(summary = "Tạo sản phẩm mới", description = "Thêm sản phẩm mới vào danh mục tương ứng")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm thành công", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sản phẩm", description = "Cập nhật thông tin chi tiết sản phẩm theo ID")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "ID sản phẩm cần sửa") @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa sản phẩm (Soft delete)", description = "Đánh dấu sản phẩm đã xóa, không còn hiển thị trên catalog")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "ID sản phẩm cần xóa") @PathVariable UUID id
    ) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }

    @PostMapping("/{id}/purchase")
    @Operation(summary = "Mua vật phẩm bằng VIP Coin", description = "Trừ VIP Coin từ ví và cấp vật phẩm vào túi đồ của người chơi")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> purchaseProduct(
            @Parameter(name = "X-User-Id", description = "UUID tài khoản người dùng", in = ParameterIn.HEADER, required = true)
            @RequestHeader("X-User-Id") UUID accountId,
            @Parameter(description = "ID sản phẩm cần mua") @PathVariable UUID id
    ) {
        InventoryItemResponse response = productService.purchaseProduct(accountId, id);
        return ResponseEntity.ok(ApiResponse.success("Mua vật phẩm thành công", response));
    }
}
