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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.penjualanrumah.model.User;

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
            // Ambil semua pesanan yang statusnya PENDING
            List<Order> pendingOrders = orderRepository.findAll().stream()
                .filter(order -> order != null && order.getStatus() != null && "PENDING".equals(order.getStatus()))
                .toList();
            
            model.addAttribute("totalOrders", pendingOrders.size());
            model.addAttribute("totalCustomers", customerRepository.count());
            // Tambahkan daftar pesanan yang PENDING ke dashboard
            model.addAttribute("orders", pendingOrders);
            return "seller_dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat dashboard: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/orders")
    public String orders(
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status, // Filter berdasarkan status
            Model model
    ) {
        try {
            // Ambil semua pesanan yang berstatus APPROVED saja
            List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order != null && order.getStatus() != null && "APPROVED".equals(order.getStatus()))
                .toList();
            
            // Filter berdasarkan status pesanan jika ada
            if (status != null && !status.isEmpty()) {
                orders = orders.stream()
                    .filter(order -> order != null && order.getStatus() != null && order.getStatus().equalsIgnoreCase(status))
                    .toList();
            }

            // Filter berdasarkan nama pelanggan
            if (customer != null && !customer.isEmpty()) {
                orders = orders.stream()
                    .filter(order -> order != null && order.getCustomer() != null && order.getCustomer().getUsername() != null && order.getCustomer().getUsername().toLowerCase().contains(customer.toLowerCase()))
                    .toList();
            }

            // Filter berdasarkan region
            if (region != null && !region.isEmpty()) {
                orders = orders.stream()
                    .filter(order -> order != null && order.getRegion() != null && order.getRegion().toString().equals(region))
                    .toList();
            }

            // Filter berdasarkan tanggal
            if (date != null && !date.isEmpty()) {
                LocalDate filterDate = LocalDate.parse(date);
                orders = orders.stream()
                    .filter(order -> order != null && order.getOrderDate() != null && order.getOrderDate().toLocalDate().equals(filterDate))
                    .toList();
            }

            // Sorting: username ASC, paymentType ASC, orderDate ASC
            orders = orders.stream()
                .sorted(java.util.Comparator.comparing((Order o) -> o.getCustomer() != null ? o.getCustomer().getUsername() : "")
                    .thenComparing(o -> o.getPaymentType() != null ? o.getPaymentType().name() : "")
                    .thenComparing(Order::getOrderDate))
                .toList();

            model.addAttribute("orders", orders);
            model.addAttribute("selectedCustomer", customer);
            model.addAttribute("selectedRegion", region);
            model.addAttribute("selectedDate", date);
            model.addAttribute("selectedStatus", status); // Tambahkan status yang dipilih ke model
            return "seller_orders";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat data pesanan: " + e.getMessage());
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
            // Filter hanya pelanggan yang punya order APPROVED/REJECTED
            customers = customers.stream()
                .filter(customer -> {
                    // Cari order dengan customer email yang sama
                    return orderRepository.findAll().stream()
                        .anyMatch(order ->
                            order != null && order.getCustomer() != null && customer.getEmail() != null && order.getCustomer().getEmail() != null && order.getCustomer().getEmail().equalsIgnoreCase(customer.getEmail()) &&
                            order.getStatus() != null && ("APPROVED".equals(order.getStatus()) || "REJECTED".equals(order.getStatus()))
                        );
                })
                .toList();
            // Filter berdasarkan nama
            if (name != null && !name.isEmpty()) {
                customers = customers.stream()
                    .filter(customer -> customer != null && customer.getName() != null && customer.getName().toLowerCase().contains(name.toLowerCase()))
                    .toList();
            }
            
            // Filter berdasarkan email
            if (email != null && !email.isEmpty()) {
                customers = customers.stream()
                    .filter(customer -> customer != null && customer.getEmail() != null && customer.getEmail().toLowerCase().contains(email.toLowerCase()))
                    .toList();
            }

            model.addAttribute("customers", customers);
            model.addAttribute("searchName", name);
            model.addAttribute("searchEmail", email);
            return "seller_customers";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat data pelanggan: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/order/{orderId}/approve")
    public String approveOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                order.setStatus("APPROVED");
                orderRepository.save(order);
                // Tambahkan/Update Customer di tabel customers
                User user = order.getCustomer();
                if (user != null && user.getEmail() != null) {
                    Customer customer = customerRepository.findAll().stream()
                        .filter(c -> c != null && c.getEmail() != null && c.getEmail().equalsIgnoreCase(user.getEmail()))
                        .findFirst().orElse(null);
                    if (customer == null) {
                        customer = new Customer();
                        customer.setName(user.getFullName());
                        customer.setEmail(user.getEmail());
                        customer.setPhone(user.getPhone());
                        customer.setJoinDate(java.time.LocalDateTime.now());
                    }
                    // Hitung ulang total pesanan yang APPROVED untuk pelanggan ini
                    long approvedOrdersCount = orderRepository.findByCustomer(user).stream()
                        .filter(o -> "APPROVED".equalsIgnoreCase(o.getStatus()))
                        .count();
                    customer.setTotalOrders((int) approvedOrdersCount);
                    customerRepository.save(customer);
                }
                redirectAttributes.addFlashAttribute("message", "Pesanan berhasil di-approve");
            }
            return "redirect:/seller/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal approve pesanan: " + e.getMessage());
            return "redirect:/seller/orders";
        }
    }

    @PostMapping("/order/{orderId}/reject")
    public String rejectOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                order.setStatus("REJECTED");
                orderRepository.save(order);
                // Tambahkan/Update Customer di tabel customers
                User user = order.getCustomer();
                if (user != null && user.getEmail() != null) {
                    Customer customer = customerRepository.findAll().stream()
                        .filter(c -> c != null && c.getEmail() != null && c.getEmail().equalsIgnoreCase(user.getEmail()))
                        .findFirst().orElse(null);
                    if (customer == null) {
                        customer = new Customer();
                        customer.setName(user.getFullName());
                        customer.setEmail(user.getEmail());
                        customer.setPhone(user.getPhone());
                        customer.setJoinDate(java.time.LocalDateTime.now());
                    }
                    // Hitung ulang total pesanan yang APPROVED untuk pelanggan ini
                    long approvedOrdersCount = orderRepository.findByCustomer(user).stream()
                        .filter(o -> "APPROVED".equalsIgnoreCase(o.getStatus()))
                        .count();
                    customer.setTotalOrders((int) approvedOrdersCount);
                    customerRepository.save(customer);
                }
                redirectAttributes.addFlashAttribute("message", "Pesanan berhasil di-reject");
            }
            return "redirect:/seller/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal reject pesanan: " + e.getMessage());
            return "redirect:/seller/orders";
        }
    }

// Endpoint untuk menampilkan daftar pelanggan
    @GetMapping("/users")
    public String users(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            Model model
    ) {
        try {
            // Ambil semua pelanggan dari database
            List<Customer> customers = customerRepository.findAll();
            
            // Filter pelanggan berdasarkan nama jika ada
            if (name != null && !name.isEmpty()) {
                customers = customers.stream()
                    .filter(customer -> customer != null && customer.getName() != null && customer.getName().toLowerCase().contains(name.toLowerCase()))
                    .toList();
            }

            // Filter pelanggan berdasarkan email jika ada
            if (email != null && !email.isEmpty()) {
                customers = customers.stream()
                    .filter(customer -> customer != null && customer.getEmail() != null && customer.getEmail().toLowerCase().contains(email.toLowerCase()))
                    .toList();
            }

            // Kirim data pelanggan ke halaman
            model.addAttribute("customers", customers);
            model.addAttribute("searchName", name);
            model.addAttribute("searchEmail", email);
            return "seller_customers";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat data pelanggan: " + e.getMessage());
            return "error";
        }
    }
}