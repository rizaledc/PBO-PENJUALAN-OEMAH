package com.penjualanrumah.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

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
    public String buyerDashboard() {
        return "buyer_dashboard";
    }
} 