package com.penjualanrumah.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import com.penjualanrumah.service.UserService;
import com.penjualanrumah.model.User;
import com.penjualanrumah.repository.OrderRepository;
import com.penjualanrumah.model.Order;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (role.equals("ROLE_SELLER")) {
            return "redirect:/seller/dashboard";
        } else if (role.equals("ROLE_BUYER")) {
            return "redirect:/buyer/dashboard";
        }
        
        return "redirect:/login";
    }

    @GetMapping("/buyer/dashboard")
    public String buyerDashboard(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.findByUsername(authentication.getName());
            if (user != null) {
                model.addAttribute("fullName", user.getFullName());
                List<Order> orders = orderRepository.findByCustomer(user);
                model.addAttribute("totalOrders", orders.size());
                long approvedCount = orders.stream().filter(o -> "APPROVED".equalsIgnoreCase(o.getStatus())).count();
                long pendingCount = orders.stream().filter(o -> "PENDING".equalsIgnoreCase(o.getStatus())).count();
                long rejectedCount = orders.stream().filter(o -> "REJECTED".equalsIgnoreCase(o.getStatus())).count();
                model.addAttribute("approvedOrders", approvedCount);
                model.addAttribute("pendingOrders", pendingCount);
                model.addAttribute("rejectedOrders", rejectedCount);
            }
        }
        return "buyer_dashboard";
    }
} 