package com.penjualanrumah.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.penjualanrumah.model.Order;
import com.penjualanrumah.model.User;
import com.penjualanrumah.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import java.math.BigDecimal;
import org.springframework.ui.Model;
import java.util.List;

@Controller
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/order")
    public String showOrderPage() {
        return "order_form";
    }
    @PostMapping("/order")
    public String handleOrder(
            @RequestParam("houseType") String houseType,
            @RequestParam("region") String region,
            @RequestParam("paymentType") String paymentType,
            @RequestParam("downPayment") int downPayment,
            @RequestParam(value = "installmentPeriod", required = false) Integer installmentPeriod,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Order order = new Order();
        order.setHouseType(Order.HouseType.valueOf(houseType));
        order.setRegion(Order.Region.valueOf(region));
        order.setPaymentType(Order.PaymentType.valueOf(paymentType));
        order.setDownPayment(BigDecimal.valueOf(downPayment));
        order.setInstallmentPeriod(installmentPeriod);
        // Set customer dari username yang login
        User user = new User();
        user.setUsername(userDetails.getUsername());
        order.setCustomer(user);
        orderRepository.save(order);
        return "redirect:/order/history";
    }

    @GetMapping("/order/history")
    public String orderHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getCustomer() != null && o.getCustomer().getUsername().equals(userDetails.getUsername()))
            .toList();
        model.addAttribute("orders", orders);
        return "order_history";
    }

    @GetMapping("/order/detail")
    public String orderDetail(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails, org.springframework.ui.Model model) {
        java.util.List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getCustomer() != null && o.getCustomer().getUsername().equals(userDetails.getUsername()))
            .toList();
        model.addAttribute("orders", orders);
        return "order_detail";
    }
}
