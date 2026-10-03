package com.utelearn.controller.admin;

import com.utelearn.entity.OrderStatus;
import com.utelearn.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

// Admin xem toàn bộ đơn hàng
@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) OrderStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("orderPage", orderService.searchForAdmin(status, from, to, keyword, page));
        model.addAttribute("statuses", OrderStatus.values());
        // giữ lại giá trị lọc trên form
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("keyword", keyword);
        return "admin/orders";
    }

    @GetMapping("/{orderCode}")
    public String detail(@PathVariable String orderCode, Model model) {
        model.addAttribute("order", orderService.findByCode(orderCode));
        return "admin/order-detail";
    }
}
