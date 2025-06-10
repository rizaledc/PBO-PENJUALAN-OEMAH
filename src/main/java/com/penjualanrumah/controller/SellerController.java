package com.penjualanrumah.controller;

import com.penjualanrumah.model.Order;
import com.penjualanrumah.model.Customer;
import com.penjualanrumah.repository.OrderRepository;
import com.penjualanrumah.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.time.LocalDate;

@Controller
@RequestMapping("/seller")
public class SellerController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Tampilkan hanya pesanan dengan status PENDING
            List<Order> orders = orderRepository.findByStatus("PENDING");
            model.addAttribute("orders", orders);
            model.addAttribute("totalOrders", orders.size());
            model.addAttribute("totalCustomers", customerRepository.count());
            return "seller_dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat dashboard");
            return "error";
        }
    }

    @PostMapping("/orders/approve")
    public String approveOrder(@RequestParam("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                order.setStatus("APPROVED");
                orderRepository.save(order);
                redirectAttributes.addFlashAttribute("message", "Pesanan berhasil di-approve.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Pesanan tidak ditemukan.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal approve pesanan.");
        }
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/orders/reject")
    public String rejectOrder(@RequestParam("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                order.setStatus("REJECTED");
                orderRepository.save(order);
                redirectAttributes.addFlashAttribute("message", "Pesanan berhasil di-reject.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Pesanan tidak ditemukan.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal reject pesanan.");
        }
        return "redirect:/seller/dashboard";
    }

    @GetMapping("/orders")
    public String orders(
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String date,
            Model model
    ) {
        try {
            List<Order> orders = orderRepository.findAll();

            // Filter berdasarkan nama pelanggan
            if (customer != null && !customer.isEmpty()) {
                orders = orders.stream()
                    .filter(order -> order.getCustomer() != null && order.getCustomer().getUsername().toLowerCase().contains(customer.toLowerCase()))
                    .toList();
            }
            // Filter berdasarkan region
            if (region != null && !region.isEmpty()) {
                orders = orders.stream()
                    .filter(order -> order.getRegion().toString().equals(region))
                    .toList();
            }
            // Filter berdasarkan tanggal
            if (date != null && !date.isEmpty()) {
                LocalDate filterDate = LocalDate.parse(date);
                orders = orders.stream()
                    .filter(order -> order.getOrderDate().toLocalDate().equals(filterDate))
                    .toList();
            }
            model.addAttribute("orders", orders);
            model.addAttribute("selectedCustomer", customer);
            model.addAttribute("selectedRegion", region);
            model.addAttribute("selectedDate", date);
            return "seller_orders";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat data pesanan");
            return "error";
        }
    }

    @GetMapping("/customers")
    public String customers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            Model model
    ) {
        try {
            List<Customer> customers = customerRepository.findAll();

            // Filter berdasarkan nama
            if (name != null && !name.isEmpty()) {
                customers = customers.stream()
                    .filter(customer -> customer.getName().toLowerCase().contains(name.toLowerCase()))
                    .toList();
            }

            // Filter berdasarkan email
            if (email != null && !email.isEmpty()) {
                customers = customers.stream()
                    .filter(customer -> customer.getEmail().toLowerCase().contains(email.toLowerCase()))
                    .toList();
            }

            model.addAttribute("customers", customers);
            model.addAttribute("searchName", name);
            model.addAttribute("searchEmail", email);
            return "seller_customers";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat data pelanggan");
            return "error";
        }
    }
}
