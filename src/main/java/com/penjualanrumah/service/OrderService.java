package com.penjualanrumah.service;

import com.penjualanrumah.model.Order;
import com.penjualanrumah.model.User;
import com.penjualanrumah.model.Order.Status;
import com.penjualanrumah.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;

import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    public List<Order> getPendingOrdersForSeller(User seller) {
        return orderRepository.findBySellerAndStatus(seller, Status.PENDING);
    }

    public void updateOrderStatus(Long orderId, Status status) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(status);
            orderRepository.save(order);
        }
    }
}
