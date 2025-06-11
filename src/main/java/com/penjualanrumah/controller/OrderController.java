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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Comparator;

@Controller
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private static final Map<Order.HouseType, BigDecimal> HOUSE_PRICES = new HashMap<>();

    static {
        HOUSE_PRICES.put(Order.HouseType.TYPE_36, new BigDecimal("150000000"));
        HOUSE_PRICES.put(Order.HouseType.TYPE_47, new BigDecimal("200000000"));
        HOUSE_PRICES.put(Order.HouseType.TYPE_57, new BigDecimal("250000000"));
    }

    private String formatCurrencyForPdf(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount);
    }

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
            
            // Set total based on house type
            BigDecimal total = HOUSE_PRICES.get(order.getHouseType());
            if (total == null) {
                throw new IllegalArgumentException("Tipe rumah tidak valid");
            }
            order.setTotal(total);

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
                .sorted(Comparator
                    .comparing(Order::getPaymentType) // Sort by PaymentType (CASH, then INSTALLMENTS as per enum order)
                    .thenComparing(Order::getOrderDate, Comparator.reverseOrder())) // Then by OrderDate (newest first)
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
            logger.info("Memulai load detail order untuk id: {}", orderId);
            
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pesanan tidak ditemukan"));
            
            logger.info("Order ditemukan: id={}, total={}, downPayment={}, installmentPeriod={}", 
                order.getId(), order.getTotal(), order.getDownPayment(), order.getInstallmentPeriod());

            if (order.getCustomer() == null) {
                logger.error("Customer pada order id {} null", orderId);
                model.addAttribute("error", "Data customer pada pesanan ini kosong.");
                return "error";
            }
            
            if (!order.getCustomer().getUsername().equals(userDetails.getUsername())) {
                logger.error("User {} mencoba akses order {} milik {}", userDetails.getUsername(), orderId, order.getCustomer().getUsername());
                model.addAttribute("error", "Anda tidak memiliki akses ke pesanan ini");
                return "error";
            }

            // Defensive programming for installment orders
            if (order.getPaymentType() == Order.PaymentType.INSTALLMENTS) {
                if (order.getInstallmentPeriod() == null || order.getInstallmentPeriod() <= 0) {
                    logger.warn("Pesanan cicilan {} memiliki installmentPeriod null atau nol. Mengatur ke 1 untuk mencegah error tampilan.", orderId);
                    order.setInstallmentPeriod(1);
                }
                
                // Log perhitungan cicilan
                if (order.getTotal() != null && order.getDownPayment() != null && order.getInstallmentPeriod() != null) {
                    BigDecimal remaining = order.getTotal().subtract(order.getDownPayment());
                    BigDecimal installmentPeriodBd = new BigDecimal(order.getInstallmentPeriod());
                    BigDecimal installmentAmount = remaining.divide(installmentPeriodBd, 0, java.math.RoundingMode.CEILING);
                    logger.info("Perhitungan cicilan: total={}, downPayment={}, remaining={}, period={}, installmentAmount={}", 
                        order.getTotal(), order.getDownPayment(), remaining, order.getInstallmentPeriod(), installmentAmount);
                }
            }

            model.addAttribute("order", order);
            logger.info("Order detail berhasil dimuat untuk id: {}", orderId);
            return "order_detail";
        } catch (Exception e) {
            logger.error("Exception saat load detail order id {}: {}", orderId, e.getMessage(), e);
            model.addAttribute("error", "Terjadi kesalahan saat memuat detail pesanan: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/order/detail/pdf")
    public void downloadOrderPdf(@RequestParam("id") Long orderId, HttpServletResponse response, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pesanan tidak ditemukan"));
            if (order.getCustomer() == null || !order.getCustomer().getUsername().equals(userDetails.getUsername())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Akses ditolak");
                return;
            }
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=order-" + orderId + ".pdf");
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());
            document.open();
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 28, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font sectionFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 20, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font labelFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
            document.add(new com.itextpdf.text.Paragraph("Detail Pesanan #" + order.getId(), titleFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph("Informasi Pesanan", sectionFont));
            document.add(new com.itextpdf.text.Paragraph("Tipe Rumah: " + (order.getHouseType() != null ? order.getHouseType().toString() : "-"), labelFont));
            document.add(new com.itextpdf.text.Paragraph("Lokasi: " + (order.getRegion() != null ? order.getRegion().toString() : "-"), labelFont));
            document.add(new com.itextpdf.text.Paragraph("Metode Pembayaran: " + (order.getPaymentType() != null ? order.getPaymentType().toString() : "-"), labelFont));
            // Status color
            String status = order.getStatus() != null ? order.getStatus() : "-";
            com.itextpdf.text.BaseColor statusColor = com.itextpdf.text.BaseColor.BLACK;
            if ("APPROVED".equalsIgnoreCase(status)) statusColor = com.itextpdf.text.BaseColor.GREEN;
            else if ("REJECTED".equalsIgnoreCase(status)) statusColor = com.itextpdf.text.BaseColor.RED;
            else if ("PENDING".equalsIgnoreCase(status)) statusColor = new com.itextpdf.text.BaseColor(255, 204, 0); // yellow
            com.itextpdf.text.Chunk statusChunk = new com.itextpdf.text.Chunk(status, new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD, statusColor));
            com.itextpdf.text.Paragraph statusParagraph = new com.itextpdf.text.Paragraph();
            statusParagraph.add(new com.itextpdf.text.Chunk("Status: ", labelFont));
            statusParagraph.add(statusChunk);
            document.add(statusParagraph);
            document.add(new com.itextpdf.text.Paragraph("Tanggal Pesanan: " + (order.getOrderDate() != null ? order.getOrderDate().toString() : "-"), labelFont));
            document.add(new com.itextpdf.text.Paragraph("Total Harga: " + (order.getTotal() != null ? formatCurrencyForPdf(order.getTotal()) : "-"), labelFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph("Detail Pembayaran", sectionFont));
            document.add(new com.itextpdf.text.Paragraph("Uang Muka: " + (order.getDownPayment() != null ? formatCurrencyForPdf(order.getDownPayment()) : "-"), labelFont));
            document.add(new com.itextpdf.text.Paragraph("Sisa Pembayaran: " + (order.getTotal() != null && order.getDownPayment() != null ? formatCurrencyForPdf(order.getTotal().subtract(order.getDownPayment())) : "-"), labelFont));
            document.add(new com.itextpdf.text.Paragraph("Jangka Waktu: " + (order.getInstallmentPeriod() != null ? order.getInstallmentPeriod() + " Bulan" : "-"), labelFont));

            String installmentAmountText = "-";
            if (order.getPaymentType() == com.penjualanrumah.model.Order.PaymentType.INSTALLMENTS &&
                order.getTotal() != null &&
                order.getDownPayment() != null &&
                order.getInstallmentPeriod() != null &&
                order.getInstallmentPeriod() > 0) {
                try {
                    BigDecimal remaining = order.getTotal().subtract(order.getDownPayment());
                    BigDecimal installmentPeriodBd = new BigDecimal(order.getInstallmentPeriod());
                    BigDecimal installmentAmount = remaining.divide(installmentPeriodBd, 0, java.math.RoundingMode.CEILING);
                    installmentAmountText = formatCurrencyForPdf(installmentAmount);
                } catch (ArithmeticException e) {
                    logger.error("Error saat menghitung cicilan per bulan untuk order id {}: {}", order.getId(), e.getMessage(), e);
                    installmentAmountText = "Error";
                }
            }
            document.add(new com.itextpdf.text.Paragraph("Cicilan per Bulan: " + installmentAmountText, labelFont));
            document.close();
        } catch (Exception e) {
            logger.error("Exception saat membuat PDF untuk order id {}: {}", orderId, e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Gagal membuat PDF: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
}