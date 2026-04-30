package com.example.marketplace.service.impl;

import com.example.marketplace.dto.*;
import com.example.marketplace.entity.*;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.MarketplaceOrderRepository;
import com.example.marketplace.repository.ProductRepository;
import com.example.marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ProductRepository productRepository;
    private final MarketplaceOrderRepository marketplaceOrderRepository;

    @Override
    @Transactional
    public OrderResponseDTO checkout(String customerEmail, Long userId, CheckoutRequestDTO request) {
        Optional<OrderResponseDTO> existingUnlock = findExistingUnlockOrder(userId, request);
        if (existingUnlock.isPresent()) {
            return existingUnlock.get();
        }

        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
        for (CheckoutItemDTO item : request.getItems()) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each line must include a product id and a positive quantity.");
            }
            quantityByProductId.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        List<Long> sortedIds = new ArrayList<>(quantityByProductId.keySet());
        Collections.sort(sortedIds);

        Map<Long, Product> lockedById = new LinkedHashMap<>();
        for (Long id : sortedIds) {
            Product p = productRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id=" + id));
            lockedById.put(id, p);
        }

        MarketplaceOrder order = MarketplaceOrder.builder()
                .customerEmail(customerEmail)
                .customerUserId(userId)
                .shippingAddress(request.getShippingAddress().trim())
                .customerNote(request.getCustomerNote())
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.MOCK_AUTHORIZED)
                .paymentReference("REQ-" + UUID.randomUUID())
                .totalAmount(BigDecimal.ZERO)
                .currency("TND")
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {
            Product product = lockedById.get(entry.getKey());
            int qty = entry.getValue();

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException("Product is not available: " + product.getName());
            }

            if (product.getType() == ProductType.PHYSICAL) {
                int available = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                if (qty > available) {
                    throw new IllegalArgumentException(
                            "Not enough stock for \"" + product.getName() + "\". Available: " + available + ", requested: " + qty + ".");
                }
            }

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(lineTotal);

            MarketplaceOrderItem orderItem = MarketplaceOrderItem.builder()
                    .product(product)
                    .quantity(qty)
                    .unitPrice(product.getPrice())
                    .lineTotal(lineTotal)
                    .build();

            order.addItem(orderItem);
        }

        for (Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {
            Product product = lockedById.get(entry.getKey());
            if (product.getType() == ProductType.PHYSICAL && product.getStockQuantity() != null) {
                product.setStockQuantity(product.getStockQuantity() - entry.getValue());
            }
        }

        order.setTotalAmount(total);

        MarketplaceOrder saved = marketplaceOrderRepository.save(order);
        return toResponse(saved);
    }

    private Optional<OrderResponseDTO> findExistingUnlockOrder(Long userId, CheckoutRequestDTO request) {
        if (userId == null || request.getItems() == null || request.getItems().size() != 1) {
            return Optional.empty();
        }

        CheckoutItemDTO checkoutItem = request.getItems().get(0);
        if (checkoutItem == null || checkoutItem.getProductId() == null) {
            return Optional.empty();
        }

        Long productId = checkoutItem.getProductId();
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getType() != ProductType.DIGITAL || !Boolean.TRUE.equals(product.getPreviewable())) {
            return Optional.empty();
        }

        return marketplaceOrderRepository.findByCustomerUserIdAndStatusOrderByCreatedAtDesc(userId, OrderStatus.PAID)
                .stream()
                .filter(order -> order.getItems().stream().anyMatch(i -> i.getProduct().getId().equals(productId)))
                .findFirst()
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(String customerEmail) {
        return marketplaceOrderRepository.findByCustomerEmailOrderByCreatedAtDesc(customerEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return marketplaceOrderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        MarketplaceOrder order = findOrderById(orderId);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatusUpdateRequestDTO request) {
        MarketplaceOrder order = marketplaceOrderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id=" + orderId));
        OrderStatus current = order.getStatus();
        OrderStatus next = request.getStatus();
        validateStatusTransition(current, next);
        if (current == OrderStatus.CREATED && next == OrderStatus.CANCELLED) {
            restorePhysicalStock(order);
        }
        order.setStatus(next);
        if (next == OrderStatus.PAID) {
            order.setPaymentReference("CONFIRMED-" + order.getId());
        }
        return toResponse(marketplaceOrderRepository.save(order));
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        MarketplaceOrder order = marketplaceOrderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id=" + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalArgumentException("Cannot cancel a confirmed order.");
        }
        restorePhysicalStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        marketplaceOrderRepository.save(order);
    }

    private void restorePhysicalStock(MarketplaceOrder order) {
        Map<Long, Product> touched = new LinkedHashMap<>();
        for (MarketplaceOrderItem item : order.getItems()) {
            Product p = item.getProduct();
            if (p.getType() == ProductType.PHYSICAL && p.getStockQuantity() != null) {
                p.setStockQuantity(p.getStockQuantity() + item.getQuantity());
                touched.put(p.getId(), p);
            }
        }
        if (!touched.isEmpty()) {
            productRepository.saveAll(touched.values());
        }
    }

    private MarketplaceOrder findOrderById(Long orderId) {
        return marketplaceOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id=" + orderId));
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled orders cannot be changed");
        }

        if (currentStatus == OrderStatus.PAID && newStatus == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel a confirmed order.");
        }

        if (currentStatus == OrderStatus.PAID && newStatus == OrderStatus.CREATED) {
            throw new IllegalArgumentException("Cannot move PAID orders back to CREATED");
        }
    }

    private OrderResponseDTO toResponse(MarketplaceOrder order) {
        List<OrderItemResponseDTO> items = order.getItems().stream()
                .map(item -> OrderItemResponseDTO.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        String message = switch (order.getStatus()) {
            case CREATED -> "Request received; awaiting confirmation (no online payment yet).";
            case PAID -> "Confirmed.";
            case CANCELLED -> "Cancelled.";
        };

        PaymentAttemptDTO payment = PaymentAttemptDTO.builder()
                .reference(order.getPaymentReference())
                .status(order.getPaymentStatus())
                .message(message)
                .build();

        return OrderResponseDTO.builder()
                .id(order.getId())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .shippingAddress(order.getShippingAddress())
                .customerNote(order.getCustomerNote())
                .paymentAttempt(payment)
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
