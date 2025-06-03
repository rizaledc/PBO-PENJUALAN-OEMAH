package com.penjualanrumah.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {
    @GetMapping("/user/order")
    public String showUserPage() {
        return "order_form";
    }
}
