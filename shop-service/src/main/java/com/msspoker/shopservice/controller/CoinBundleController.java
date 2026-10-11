package com.msspoker.shopservice.controller;

import com.msspoker.shopservice.dto.response.ApiResponse;
import com.msspoker.shopservice.dto.response.CoinBundleResponse;
import com.msspoker.shopservice.service.CoinBundleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shop/bundles")
@RequiredArgsConstructor
@Tag(name = "Coin Bundles APIs", description = "Các API hiển thị danh sách gói nạp VIP Coin theo chuẩn Riot Games")
public class CoinBundleController {

    private final CoinBundleService coinBundleService;

    @GetMapping
    @Operation(summary = "Lấy danh sách gói nạp VIP Coin", description = "Lấy danh sách các gói nạp đang mở bán có sắp xếp theo thứ tự ưu tiên (hỗ trợ Redis Cache)")
    public ResponseEntity<ApiResponse<List<CoinBundleResponse>>> getActiveBundles() {
        List<CoinBundleResponse> bundles = coinBundleService.getActiveBundles();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gói nạp thành công", bundles));
    }
}
