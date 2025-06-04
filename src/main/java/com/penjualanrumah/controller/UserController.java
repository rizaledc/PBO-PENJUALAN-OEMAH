package com.penjualanrumah.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.penjualanrumah.service.UserService;
import com.penjualanrumah.repository.OrderRepository;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.penjualanrumah.model.Order;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> userProfile(@AuthenticationPrincipal UserDetails userDetails) {
        var user = userService.findByUsername(userDetails.getUsername());
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);

        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getCustomer() != null && o.getCustomer().getUsername().equals(userDetails.getUsername()))
            .toList();
        response.put("orders", orders);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getUserOrders(@AuthenticationPrincipal UserDetails userDetails) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getCustomer() != null && o.getCustomer().getUsername().equals(userDetails.getUsername()))
            .toList();
        return ResponseEntity.ok(orders);
    }
}
