package com.msspoker.shopservice.controller;

import com.msspoker.shopservice.dto.request.CreateProductRequest;
import com.msspoker.shopservice.dto.request.UpdateProductRequest;
import com.msspoker.shopservice.dto.response.ApiResponse;
import com.msspoker.shopservice.dto.response.PageResponse;
import com.msspoker.shopservice.dto.response.ProductResponse;
import com.msspoker.shopservice.enums.ProductTier;
import com.msspoker.shopservice.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController controller;

    @Test
    @DisplayName("getProducts gọi ProductService với các tham số lọc và phân trang")
    void getProducts() {
        UUID accountId = UUID.randomUUID();
        PageResponse<ProductResponse> pageResponse = PageResponse.<ProductResponse>builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();

        when(productService.getProducts(eq("FRAME"), eq(ProductTier.VIP), any(Pageable.class), eq(accountId)))
                .thenReturn(pageResponse);

        ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> result = controller.getProducts(
                "FRAME",
                ProductTier.VIP,
                PageRequest.of(0, 20),
                accountId
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(pageResponse);
        verify(productService).getProducts(eq("FRAME"), eq(ProductTier.VIP), any(Pageable.class), eq(accountId));
    }

    @Test
    @DisplayName("getProductDetail gọi ProductService và trả về thông tin chi tiết sản phẩm")
    void getProductDetail() {
        UUID productId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        ProductResponse response = ProductResponse.builder()
                .id(productId)
                .name("Khung Rồng")
                .isOwned(true)
                .build();

        when(productService.getProductDetail(productId, accountId)).thenReturn(response);

        ResponseEntity<ApiResponse<ProductResponse>> result = controller.getProductDetail(productId, accountId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(response);
        verify(productService).getProductDetail(productId, accountId);
    }

    @Test
    @DisplayName("createProduct gọi ProductService và trả về 201 Created")
    void createProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(UUID.randomUUID())
                .name("Khung Rồng")
                .tier(ProductTier.VIP)
                .priceVnd(50000L)
                .assetUrl("https://example.com/frame.png")
                .build();
        ProductResponse response = ProductResponse.builder().name("Khung Rồng").build();

        when(productService.createProduct(request)).thenReturn(response);

        ResponseEntity<ApiResponse<ProductResponse>> result = controller.createProduct(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(response);
        verify(productService).createProduct(request);
    }

    @Test
    @DisplayName("updateProduct gọi ProductService và trả về 200 OK")
    void updateProduct() {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = UpdateProductRequest.builder()
                .categoryId(UUID.randomUUID())
                .name("Khung Rồng Update")
                .tier(ProductTier.VIP)
                .priceVnd(60000L)
                .assetUrl("https://example.com/frame.png")
                .build();
        ProductResponse response = ProductResponse.builder().name("Khung Rồng Update").build();

        when(productService.updateProduct(id, request)).thenReturn(response);

        ResponseEntity<ApiResponse<ProductResponse>> result = controller.updateProduct(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(response);
        verify(productService).updateProduct(id, request);
    }

    @Test
    @DisplayName("deleteProduct gọi ProductService và trả về 200 OK")
    void deleteProduct() {
        UUID id = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> result = controller.deleteProduct(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).deleteProduct(id);
    }
}
