package com.penjualanrumah.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.penjualanrumah.model.Order;
import com.penjualanrumah.model.User;
import com.penjualanrumah.repository.OrderRepository;
import com.penjualanrumah.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import java.math.BigDecimal;
import org.springframework.ui.Model;
import java.util.List;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/order")
    public String showOrderPage(Model model) {
        try {
            model.addAttribute("houseTypes", Order.HouseType.values());
            model.addAttribute("regions", Order.Region.values());
            model.addAttribute("paymentTypes", Order.PaymentType.values());
            return "order_form";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat halaman pesanan");
            return "error";
        }
    }

    @PostMapping("/order")
    public String handleOrder(
            @RequestParam("houseType") String houseType,
            @RequestParam("region") String region,
            @RequestParam("paymentType") String paymentType,
            @RequestParam("downPayment") int downPayment,
            @RequestParam(value = "installmentPeriod", required = false) Integer installmentPeriod,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Order order = new Order();
            order.setHouseType(Order.HouseType.valueOf(houseType));
            order.setRegion(Order.Region.valueOf(region));
            order.setPaymentType(Order.PaymentType.valueOf(paymentType));
            order.setDownPayment(BigDecimal.valueOf(downPayment));
            order.setInstallmentPeriod(installmentPeriod);
            
            User user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User tidak ditemukan. Silakan login ulang.");
                return "redirect:/order";
            }
            order.setCustomer(user);
            order.setStatus("PENDING");
            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("message", "Pesanan berhasil dibuat");
            return "redirect:/order/history";
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Data yang dimasukkan tidak valid: " + e.getMessage());
            return "redirect:/order";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan saat memproses pesanan: " + e.getMessage());
            return "redirect:/order";
        }
    }

    @GetMapping("/order/history")
    public String orderHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                model.addAttribute("error", "User tidak ditemukan");
                return "error";
            }

            List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getCustomer() != null && o.getCustomer().getUsername().equals(userDetails.getUsername()))
                .toList();
            model.addAttribute("orders", orders);
            return "order_history";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat riwayat pesanan");
            return "error";
        }
    }

    @GetMapping("/order/detail")
    public String orderDetail(
            @RequestParam("id") Long orderId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pesanan tidak ditemukan"));

            if (!order.getCustomer().getUsername().equals(userDetails.getUsername())) {
                model.addAttribute("error", "Anda tidak memiliki akses ke pesanan ini");
                return "error";
            }

            model.addAttribute("order", order);
            return "order_detail";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat detail pesanan");
            return "error";
        }
    }
}
