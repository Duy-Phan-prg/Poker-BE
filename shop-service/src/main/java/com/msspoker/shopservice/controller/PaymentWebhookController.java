package com.msspoker.shopservice.controller;

import com.msspoker.shopservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.Webhook;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments/payos")
@RequiredArgsConstructor
@Tag(name = "Payment Webhook APIs", description = "Endpoint tiếp nhận webhook từ cổng thanh toán PayOS (Public, xác thực chữ ký )")
public class PaymentWebhookController {

    private final OrderService orderService;

    @PostMapping("/webhook")
    @Operation(summary = "Tiếp nhận webhook thanh toán từ PayOS", description = "Xử lý thông báo biến động giao dịch từ PayOS với chữ ký và cơ chế Idempotency")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Webhook webhook) {
        log.info("Incoming PayOS webhook notification: code={}, desc={}", webhook.getCode(), webhook.getDesc());
        orderService.handlePayosWebhook(webhook);
        return ResponseEntity.ok(Map.of(
                "error", 0,
                "message", "Ok",
                "data", Map.of()
        ));
    }
}
