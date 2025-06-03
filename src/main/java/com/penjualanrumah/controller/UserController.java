package com.penjualanrumah.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import com.penjualanrumah.service.UserService;
import com.penjualanrumah.repository.OrderRepository;
import java.util.List;
import com.penjualanrumah.model.Order;

@Controller
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/user/order")
    public String showUserPage() {
        return "order_form";
    }

    @GetMapping("/user/profile")
    public String userProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);

        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getCustomer() != null && o.getCustomer().getUsername().equals(userDetails.getUsername()))
            .toList();
        model.addAttribute("orders", orders);

        return "profile";
    }
}
