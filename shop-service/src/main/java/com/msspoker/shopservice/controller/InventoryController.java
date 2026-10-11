package com.msspoker.shopservice.controller;

import com.msspoker.shopservice.dto.response.ApiResponse;
import com.msspoker.shopservice.dto.response.InventoryItemResponse;
import com.msspoker.shopservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Public Inventory APIs", description = "Các API túi đồ của người dùng (yêu cầu header X-User-Id do Gateway đính kèm)")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/me")
    @Operation(summary = "Xem túi đồ của tôi", description = "Lấy danh sách các vật phẩm hiện có trong túi đồ của người dùng")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getMyInventory(
            @Parameter(name = "X-User-Id", description = "UUID tài khoản người dùng", in = ParameterIn.HEADER, required = true)
            @RequestHeader("X-User-Id") UUID accountId
    ) {
        List<InventoryItemResponse> items = inventoryService.getMyInventory(accountId);
        return ResponseEntity.ok(ApiResponse.success("Lấy túi đồ thành công", items));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Xem chi tiết vật phẩm trong túi đồ", description = "Lấy thông tin chi tiết một vật phẩm mà người dùng đang sở hữu")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getMyInventoryItem(
            @Parameter(name = "X-User-Id", description = "UUID tài khoản người dùng", in = ParameterIn.HEADER, required = true)
            @RequestHeader("X-User-Id") UUID accountId,
            @Parameter(description = "ID của sản phẩm trong túi đồ")
            @PathVariable UUID productId
    ) {
        InventoryItemResponse response = inventoryService.getMyInventoryItem(accountId, productId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết vật phẩm thành công", response));
    }

    @PutMapping("/{productId}/equip")
    @Operation(summary = "Trang bị vật phẩm", description = "Trang bị vật phẩm vào ô tương ứng (tự động gỡ vật phẩm cũ cùng danh mục)")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> equipItem(
            @Parameter(name = "X-User-Id", description = "UUID tài khoản người dùng", in = ParameterIn.HEADER, required = true)
            @RequestHeader("X-User-Id") UUID accountId,
            @Parameter(description = "ID của sản phẩm cần trang bị")
            @PathVariable UUID productId
    ) {
        InventoryItemResponse response = inventoryService.equipItem(accountId, productId);
        return ResponseEntity.ok(ApiResponse.success("Trang bị vật phẩm thành công", response));
    }

    @DeleteMapping("/{productId}/equip")
    @Operation(summary = "Tháo trang bị vật phẩm", description = "Gỡ trạng thái trang bị của một vật phẩm trong túi đồ")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> unequipItem(
            @Parameter(name = "X-User-Id", description = "UUID tài khoản người dùng", in = ParameterIn.HEADER, required = true)
            @RequestHeader("X-User-Id") UUID accountId,
            @Parameter(description = "ID của sản phẩm cần tháo trang bị")
            @PathVariable UUID productId
    ) {
        InventoryItemResponse response = inventoryService.unequipItem(accountId, productId);
        return ResponseEntity.ok(ApiResponse.success("Tháo trang bị vật phẩm thành công", response));
    }
}
