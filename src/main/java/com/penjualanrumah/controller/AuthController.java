package com.penjualanrumah.controller;

import com.penjualanrumah.model.User;
import com.penjualanrumah.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("error", "Username atau password salah");
        }
        if (logout != null) {
            model.addAttribute("message", "Anda telah berhasil logout");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user,
                             @RequestParam("confirmPassword") String confirmPassword,
                             @RequestParam(value = "sellerCode", required = false) String sellerCode,
                             RedirectAttributes redirectAttributes) {
        try {
            if (!user.getPassword().equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Password tidak cocok");
                return "redirect:/register";
            }

            if (user.getRole() == User.UserRole.SELLER) {
                if (sellerCode == null || !sellerCode.equals("MATA DUITAN")) {
                    redirectAttributes.addFlashAttribute("error", "Kode Seller tidak valid");
                    return "redirect:/register";
                }
            }

            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("message", "Registrasi berhasil! Silakan login.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/login?logout";
    }
} 