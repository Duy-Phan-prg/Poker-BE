package com.msspoker.shopservice.scheduler;

import com.msspoker.shopservice.entity.InventoryItem;
import com.msspoker.shopservice.entity.Order;
import com.msspoker.shopservice.enums.ItemSource;
import com.msspoker.shopservice.enums.OrderStatus;
import com.msspoker.shopservice.repository.InventoryItemRepository;
import com.msspoker.shopservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OrderReconciliationScheduler {

    private final OrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PayOS payOS;
    private final com.msspoker.shopservice.producer.TopupEventProducer topupEventProducer;

    /**
     * Chạy định kỳ mỗi 5 phút để đối soát các đơn hàng PENDING đã quá thời gian 15 phút.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcileExpiredOrders() {
        Instant now = Instant.now();
        List<Order> expiredOrders = orderRepository.findByStatusAndExpiresAtBefore(OrderStatus.PENDING, now);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} pending orders that reached expiration. Reconciling with PayOS...", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                reconcileSingleOrder(order.getId());
            } catch (Exception ex) {
                log.error("Error reconciling order id={}, payosCode={}: {}",
                        order.getId(), order.getPayosOrderCode(), ex.getMessage());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reconcileSingleOrder(java.util.UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        try {
            PaymentLink link = payOS.paymentRequests().get(order.getPayosOrderCode());
            PaymentLinkStatus status = link.getStatus();

            if (PaymentLinkStatus.PAID.equals(status)) {
                log.info("Order code={} was PAID on PayOS but pending in DB. Fulfilling order now.", order.getPayosOrderCode());
                order.setStatus(OrderStatus.PAID);
                order.setPaidAt(Instant.now());
                orderRepository.save(order);

                if (order.getBundle() != null) {
                    long vipCoinAmount = order.getVipCoinAmount() != null
                            ? order.getVipCoinAmount()
                            : order.getBundle().getVipCoinAmount();

                    com.msspoker.shopservice.event.VipCoinTopupSuccessEvent event = com.msspoker.shopservice.event.VipCoinTopupSuccessEvent.builder()
                            .orderId(order.getId())
                            .accountId(order.getAccountId())
                            .bundleId(order.getBundle().getId())
                            .vipCoinAmount(vipCoinAmount)
                            .payosOrderCode(order.getPayosOrderCode())
                            .source("PAYOS_BUNDLE")
                            .timestamp(Instant.now())
                            .build();

                    topupEventProducer.sendTopupSuccessEvent(event);
                    log.info("Fulfilled bundle top-up for reconciled order {}", order.getId());
                } else if (order.getProduct() != null) {
                    boolean alreadyOwned = inventoryItemRepository.existsByAccountIdAndProductId(
                            order.getAccountId(), order.getProduct().getId()
                    );
                    if (!alreadyOwned) {
                        InventoryItem item = InventoryItem.builder()
                                .accountId(order.getAccountId())
                                .product(order.getProduct())
                                .source(ItemSource.PURCHASE)
                                .isEquipped(false)
                                .grantKey("ORDER:" + order.getId())
                                .build();
                        inventoryItemRepository.save(item);
                        log.info("Fulfilled item for reconciled order {}", order.getId());
                    }
                }
            } else {
                log.info("Expiring order code={} (PayOS status: {})", order.getPayosOrderCode(), status);
                order.setStatus(OrderStatus.EXPIRED);
                orderRepository.save(order);

                if (PaymentLinkStatus.PENDING.equals(status)) {
                    try {
                        payOS.paymentRequests().cancel(order.getPayosOrderCode(), "Expired");
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to query PayOS for orderCode={}. Marking as EXPIRED locally. Error: {}",
                    order.getPayosOrderCode(), ex.getMessage());
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
        }
    }
}
