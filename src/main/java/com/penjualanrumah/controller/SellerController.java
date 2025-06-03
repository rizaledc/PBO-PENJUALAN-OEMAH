package com.penjualanrumah.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SellerController {
    @GetMapping("/seller/dashboard")
    public String showSellerPage() {
        return "seller_dashboard";
    }
}
