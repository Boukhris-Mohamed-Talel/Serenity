package com.example.marketplace.repository;

import com.example.marketplace.entity.MarketplaceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, Long> {

    List<MarketplaceOrder> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
}
