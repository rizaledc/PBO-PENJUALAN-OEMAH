package com.penjualanrumah.controller;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        String redirectUrl = role.equals("ROLE_SELLER") ? "/api/seller/dashboard" : "/api/buyer/dashboard";
        
        return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
    }

    @GetMapping("/buyer/dashboard")
    public ResponseEntity<?> buyerDashboard() {
        return ResponseEntity.ok(Map.of("message", "Welcome to Buyer Dashboard"));
    }

    @GetMapping("/seller/dashboard")
    public ResponseEntity<?> sellerDashboard() {
        return ResponseEntity.ok(Map.of("message", "Welcome to Seller Dashboard"));
    }
} 