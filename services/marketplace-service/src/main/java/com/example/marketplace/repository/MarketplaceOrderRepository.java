package com.example.marketplace.repository;

import com.example.marketplace.entity.MarketplaceOrder;
import com.example.marketplace.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, Long> {

    List<MarketplaceOrder> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    List<MarketplaceOrder> findAllByOrderByCreatedAtDesc();

    List<MarketplaceOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<MarketplaceOrder> findByCustomerUserIdAndStatusOrderByCreatedAtDesc(Long customerUserId, OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("SELECT o FROM MarketplaceOrder o WHERE o.id = :id")
    Optional<MarketplaceOrder> findWithItemsById(@Param("id") Long id);

    @Query(
            "SELECT DISTINCT o.customerUserId FROM MarketplaceOrder o JOIN o.items i "
                    + "WHERE i.product.id = :productId AND o.status = :status AND o.customerUserId IS NOT NULL")
    Set<Long> findCustomerUserIdsWithPaidPurchaseForProduct(
            @Param("productId") Long productId, @Param("status") OrderStatus status);
}
