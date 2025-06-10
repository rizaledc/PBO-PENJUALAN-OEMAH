package com.penjualanrumah.controller;

import com.penjualanrumah.repository.OrderRepository;
import com.penjualanrumah.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.penjualanrumah.model.Order;
import com.penjualanrumah.model.Customer;
import java.util.List;
import java.time.LocalDate;

@Controller
@RequestMapping("/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @ModelAttribute
    public void addAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("currentUser", auth.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Tambahkan data yang diperlukan untuk dashboard
            model.addAttribute("totalOrders", orderRepository.count());
            model.addAttribute("totalCustomers", customerRepository.count());
            return "seller_dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat dashboard");
            return "error";
        }
    }

    @GetMapping("/orders")
    public String orders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String date,
            Model model
    ) {
        try {
            List<Order> orders = orderRepository.findAll();
            
            // Filter berdasarkan status
            if (status != null && !status.isEmpty()) {
                orders = orders.stream()
                    .filter(order -> order.getStatus().equals(status))
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
            model.addAttribute("selectedStatus", status);
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
