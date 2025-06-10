package com.penjualanrumah.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import com.penjualanrumah.service.UserService;
import com.penjualanrumah.repository.OrderRepository;
import java.util.List;
import com.penjualanrumah.model.Order;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@PreAuthorize("isAuthenticated()")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/user/order")
    public String showUserPage(
        @RequestParam(value = "houseType", required = false) String houseType,
        @RequestParam(value = "region", required = false) String region,
        Model model
    ) {
        try {
            model.addAttribute("selectedHouseType", houseType);
            model.addAttribute("selectedRegion", region);
            return "order_form";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat halaman pesanan");
            return "error";
        }
    }

    @GetMapping("/profile")
    public String userProfile(@AuthenticationPrincipal UserDetails userDetails, Model model,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        try {
            var user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                model.addAttribute("error", "User tidak ditemukan");
                return "error";
            }
            model.addAttribute("user", user);
            // GUNAKAN findByCustomer AGAR QUERY SESUAI USER
            List<Order> orders = orderRepository.findByCustomer(user);
            int pageSize = 3;
            int totalPages = (int) Math.ceil((double) orders.size() / pageSize);
            if (page < 0)
                page = 0;
            if (page >= totalPages && totalPages > 0)
                page = totalPages - 1;
            int fromIndex = page * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, orders.size());
            List<Order> ordersPage = (orders.size() == 0 || fromIndex >= orders.size()) ? List.of()
                    : orders.subList(fromIndex, toIndex);
            boolean hasNext = toIndex < orders.size();
            model.addAttribute("ordersPage", ordersPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("hasNext", hasNext);
            return "profile";
        } catch (Exception e) {
            model.addAttribute("error", "Terjadi kesalahan saat memuat profil: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/profile/change-password")
    public String changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes
    ) {
        try {
            var user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User tidak ditemukan");
                return "redirect:/profile";
            }

            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Password lama salah");
                return "redirect:/profile";
            }

            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Konfirmasi password tidak cocok");
                return "redirect:/profile";
            }

            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Password baru minimal 6 karakter");
                return "redirect:/profile";
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userService.save(user);
            redirectAttributes.addFlashAttribute("message", "Password berhasil diubah");
            return "redirect:/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan saat mengubah password");
            return "redirect:/profile";
        }
    }
}
