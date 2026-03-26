package com.example.marketplace.service;

import com.example.marketplace.dto.CheckoutRequestDTO;
import com.example.marketplace.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {

    OrderResponseDTO checkout(String customerEmail, Long userId, CheckoutRequestDTO request);

    List<OrderResponseDTO> getMyOrders(String customerEmail);
}
