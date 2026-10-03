package com.utelearn.controller;

import com.utelearn.entity.Order;
import com.utelearn.entity.OrderStatus;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.OrderService;
import com.utelearn.service.UserService;
import com.utelearn.service.VnPayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

// Thanh toán + lịch sử đơn hàng
@Controller
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final VnPayService vnPayService;

    public OrderController(OrderService orderService, UserService userService, VnPayService vnPayService) {
        this.orderService = orderService;
        this.userService = userService;
        this.vnPayService = vnPayService;
    }

    // Bước 1: tạo đơn từ giỏ hàng
    @PostMapping("/checkout")
    public String checkout(@RequestParam(required = false) String couponCode,
                           Principal principal, RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        try {
            Order order = orderService.checkout(user, couponCode);
            return "redirect:/payment/" + order.getOrderCode();
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    // Bước 2: trang cổng thanh toán (giả lập)
    @GetMapping("/payment/{orderCode}")
    public String paymentPage(@PathVariable String orderCode, Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        Order order = orderService.getOrderOfUser(orderCode, user);
        if (order.getStatus() != OrderStatus.PENDING) {
            return "redirect:/orders/" + orderCode;
        }
        model.addAttribute("order", order);
        model.addAttribute("vnpayEnabled", vnPayService.isEnabled());
        return "order/payment";
    }

    // Bước 3a: xác nhận thanh toán -> ghi danh
    @PostMapping("/payment/{orderCode}/confirm")
    public String confirmPayment(@PathVariable String orderCode, Principal principal, RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        try {
            orderService.confirmMockPayment(orderCode, user);
            redirect.addFlashAttribute("success", "Thanh toán thành công! Các khóa học đã được thêm vào \"Khóa học của tôi\"");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + orderCode;
    }

    // Bước 3b: hủy thanh toán
    @PostMapping("/payment/{orderCode}/cancel")
    public String cancelPayment(@PathVariable String orderCode, Principal principal, RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        try {
            orderService.cancel(orderCode, user);
            redirect.addFlashAttribute("success", "Đã hủy đơn hàng");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + orderCode;
    }

    @GetMapping("/orders")
    public String myOrders(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("orders", orderService.getOrdersOfUser(user));
        return "order/list";
    }

    @GetMapping("/orders/{orderCode}")
    public String orderDetail(@PathVariable String orderCode, Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("order", orderService.getOrderOfUser(orderCode, user));
        return "order/detail";
    }
}
