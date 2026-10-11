package com.msspoker.shopservice.service.impl;

import com.msspoker.shopservice.dto.request.CreateOrderRequest;
import com.msspoker.shopservice.dto.response.OrderResponse;
import com.msspoker.shopservice.entity.CoinBundle;
import com.msspoker.shopservice.entity.InventoryItem;
import com.msspoker.shopservice.entity.Order;
import com.msspoker.shopservice.entity.Product;
import com.msspoker.shopservice.enums.ItemSource;
import com.msspoker.shopservice.enums.OrderStatus;
import com.msspoker.shopservice.event.VipCoinTopupSuccessEvent;
import com.msspoker.shopservice.exception.OrderExceptions;
import com.msspoker.shopservice.exception.ProductExceptions;
import com.msspoker.shopservice.mapper.OrderMapper;
import com.msspoker.shopservice.producer.TopupEventProducer;
import com.msspoker.shopservice.repository.CoinBundleRepository;
import com.msspoker.shopservice.repository.InventoryItemRepository;
import com.msspoker.shopservice.repository.OrderRepository;
import com.msspoker.shopservice.repository.ProductRepository;
import com.msspoker.shopservice.service.OrderService;
import com.msspoker.shopservice.validator.CreateOrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.exception.InvalidSignatureException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CoinBundleRepository coinBundleRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final OrderMapper orderMapper;
    private final List<CreateOrderValidator> createOrderValidators;
    private final PayOS payOS;
    private final TopupEventProducer topupEventProducer;

    @Value("${payos.return-url:http://localhost:5173/shop/payment/success}")
    private String returnUrl;

    @Value("${payos.cancel-url:http://localhost:5173/shop/payment/cancel}")
    private String cancelUrl;

    @Value("${shop.order.expire-minutes:15}")
    private int expireMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(UUID accountId, CreateOrderRequest request) {
        log.info("Initiating order creation for accountId={}, productId={}", accountId, request.productId());

        Product product = productRepository.findByIdAndIsDeletedFalse(request.productId())
                .orElseThrow(ProductExceptions::productNotFound);

        // Run all strategy validators (purchasable, not owned, no active pending order)
        createOrderValidators.forEach(v -> v.validate(accountId, request, product));

        // Generate atomic unique sequence-based PayOS order code
        Long orderCode = orderRepository.generatePayosOrderCode();
        if (orderCode == null) {
            orderCode = System.currentTimeMillis();
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(expireMinutes));

        // Short alphanumeric description for PayOS (must not exceed 25 chars)
        String description = "MSS " + orderCode;

        PaymentLinkItem item = PaymentLinkItem.builder()
                .name(product.getName())
                .quantity(1)
                .price(product.getPriceVnd())
                .build();

        CreatePaymentLinkRequest payosRequest = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(product.getPriceVnd())
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .items(List.of(item))
                .expiredAt(expiresAt.getEpochSecond())
                .build();

        CreatePaymentLinkResponse payosResponse;
        try {
            log.info("Requesting PayOS payment link for orderCode={}, amount={}", orderCode, product.getPriceVnd());
            payosResponse = payOS.paymentRequests().create(payosRequest);
        } catch (Exception ex) {
            log.error("Failed to create PayOS payment link for orderCode={}: {}", orderCode, ex.getMessage(), ex);
            throw OrderExceptions.payosServiceError(ex.getMessage());
        }

        Order order = Order.builder()
                .accountId(accountId)
                .product(product)
                .amountVnd(product.getPriceVnd())
                .payosOrderCode(orderCode)
                .checkoutUrl(payosResponse.getCheckoutUrl())
                .qrCode(payosResponse.getQrCode())
                .status(OrderStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with id={}, payosOrderCode={}, expiresAt={}",
                savedOrder.getId(), savedOrder.getPayosOrderCode(), expiresAt);

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createBundleOrder(UUID accountId, UUID bundleId) {
        log.info("Initiating bundle order creation for accountId={}, bundleId={}", accountId, bundleId);

        CoinBundle bundle = coinBundleRepository.findByIdAndIsActiveTrue(bundleId)
                .orElseThrow(OrderExceptions::bundleNotFound);

        Long orderCode = orderRepository.generatePayosOrderCode();
        if (orderCode == null) {
            orderCode = System.currentTimeMillis();
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(expireMinutes));

        String description = "MSS VIP " + orderCode;
        if (description.length() > 25) {
            description = description.substring(0, 25);
        }

        PaymentLinkItem item = PaymentLinkItem.builder()
                .name(bundle.getName())
                .quantity(1)
                .price(bundle.getPriceVnd())
                .build();

        CreatePaymentLinkRequest payosRequest = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(bundle.getPriceVnd())
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .items(List.of(item))
                .expiredAt(expiresAt.getEpochSecond())
                .build();

        CreatePaymentLinkResponse payosResponse;
        try {
            log.info("Requesting PayOS payment link for bundle orderCode={}, amount={}", orderCode, bundle.getPriceVnd());
            payosResponse = payOS.paymentRequests().create(payosRequest);
        } catch (Exception ex) {
            log.error("Failed to create PayOS payment link for bundle orderCode={}: {}", orderCode, ex.getMessage(), ex);
            throw OrderExceptions.payosServiceError(ex.getMessage());
        }

        Order order = Order.builder()
                .accountId(accountId)
                .bundle(bundle)
                .vipCoinAmount(bundle.getVipCoinAmount())
                .amountVnd(bundle.getPriceVnd())
                .payosOrderCode(orderCode)
                .checkoutUrl(payosResponse.getCheckoutUrl())
                .qrCode(payosResponse.getQrCode())
                .status(OrderStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Bundle order created successfully with id={}, payosOrderCode={}, vipCoinAmount={}",
                savedOrder.getId(), savedOrder.getPayosOrderCode(), savedOrder.getVipCoinAmount());

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderDetail(UUID accountId, UUID orderId) {
        log.debug("Fetching order detail for accountId={}, orderId={}", accountId, orderId);
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getAccountId().equals(accountId))
                .orElseThrow(OrderExceptions::orderNotFound);
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getMyOrders(UUID accountId) {
        log.debug("Fetching orders list for accountId={}", accountId);
        List<Order> orders = orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        return orderMapper.toOrderResponseList(orders);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse cancelOrder(UUID accountId, UUID orderId) {
        log.info("Cancelling order orderId={} by accountId={}", orderId, accountId);
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getAccountId().equals(accountId))
                .orElseThrow(OrderExceptions::orderNotFound);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw OrderExceptions.orderNotPending();
        }

        try {
            payOS.paymentRequests().cancel(order.getPayosOrderCode(), "User cancelled");
        } catch (Exception ex) {
            log.warn("Could not cancel order on PayOS gateway for orderCode={}: {}",
                    order.getPayosOrderCode(), ex.getMessage());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        log.info("Order id={} successfully marked as CANCELLED", orderId);
        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayosWebhook(Webhook webhook) {
        log.info("Received PayOS webhook with code={}, desc={}", webhook.getCode(), webhook.getDesc());

        // 1. Verify HMAC-SHA256 signature
        WebhookData data;
        try {
            data = payOS.webhooks().verify(webhook);
        } catch (InvalidSignatureException ex) {
            log.error("Invalid PayOS webhook signature detected: {}", ex.getMessage());
            throw OrderExceptions.invalidWebhookSignature();
        } catch (Exception ex) {
            log.error("PayOS webhook verification failed: {}", ex.getMessage(), ex);
            throw OrderExceptions.invalidWebhookSignature();
        }

        Long orderCode = data.getOrderCode();
        log.info("Webhook verified successfully for orderCode={}, amount={}", orderCode, data.getAmount());

        // 2. Acquire Pessimistic Write Lock (SELECT ... FOR UPDATE) to prevent race conditions & deadlocks
        Order order = orderRepository.findByPayosOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> {
                    log.error("Order not found for PayOS orderCode={}", orderCode);
                    return OrderExceptions.orderNotFound();
                });

        // 3. Idempotency Check: if already paid, acknowledge 200 without duplicate side effects
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order code={} is already marked as PAID. Acknowledging webhook idempotently.", orderCode);
            return;
        }

        // 4. Handle successful payment code ("00")
        if ("00".equals(webhook.getCode())) {
            // Verify payment amount
            if (data.getAmount() < order.getAmountVnd()) {
                log.error("Amount mismatch for orderCode={}: expected={}, received={}",
                        orderCode, order.getAmountVnd(), data.getAmount());
                return;
            }

            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(Instant.now());
            orderRepository.save(order);

            // 5. Fulfillment based on order type (Bundle Top-up vs Item Purchase)
            if (order.getBundle() != null) {
                long vipCoinAmount = order.getVipCoinAmount() != null
                        ? order.getVipCoinAmount()
                        : order.getBundle().getVipCoinAmount();

                VipCoinTopupSuccessEvent event = VipCoinTopupSuccessEvent.builder()
                        .orderId(order.getId())
                        .accountId(order.getAccountId())
                        .bundleId(order.getBundle().getId())
                        .vipCoinAmount(vipCoinAmount)
                        .payosOrderCode(order.getPayosOrderCode())
                        .source("PAYOS_BUNDLE")
                        .timestamp(Instant.now())
                        .build();

                topupEventProducer.sendTopupSuccessEvent(event);
                log.info("Published VIP coin top-up event for bundle orderId={}, accountId={}, vipCoin={}",
                        order.getId(), order.getAccountId(), vipCoinAmount);
            } else if (order.getProduct() != null) {
                String grantKey = "ORDER:" + order.getId();
                boolean alreadyOwned = inventoryItemRepository.existsByAccountIdAndProductId(
                        order.getAccountId(), order.getProduct().getId()
                );

                if (!alreadyOwned) {
                    InventoryItem item = InventoryItem.builder()
                            .accountId(order.getAccountId())
                            .product(order.getProduct())
                            .source(ItemSource.PURCHASE)
                            .isEquipped(false)
                            .grantKey(grantKey)
                            .build();
                    inventoryItemRepository.save(item);
                    log.info("Granted productId={} to accountId={} from orderId={}",
                            order.getProduct().getId(), order.getAccountId(), order.getId());
                } else {
                    log.warn("Account {} already owns product {} when fulfilling order {}. Skipping duplicate insert.",
                            order.getAccountId(), order.getProduct().getId(), order.getId());
                }
            }
        } else {
            log.warn("Payment webhook received non-success code={} for orderCode={}", webhook.getCode(), orderCode);
        }
    }
}
