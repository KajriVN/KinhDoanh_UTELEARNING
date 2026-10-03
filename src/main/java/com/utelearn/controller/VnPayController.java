package com.utelearn.controller;

import com.utelearn.entity.Order;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.OrderService.VnPayResult;
import com.utelearn.service.OrderService;
import com.utelearn.service.UserService;
import com.utelearn.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

// Thanh toán qua VNPay sandbox
@Controller
public class VnPayController {

    private final VnPayService vnPayService;
    private final OrderService orderService;
    private final UserService userService;

    public VnPayController(VnPayService vnPayService, OrderService orderService, UserService userService) {
        this.vnPayService = vnPayService;
        this.orderService = orderService;
        this.userService = userService;
    }

    // Bấm "Thanh toán VNPay" -> đổi phương thức đơn sang VNPAY -> chuyển sang trang VNPay
    @PostMapping("/payment/{orderCode}/vnpay")
    public String start(@PathVariable String orderCode, Principal principal, HttpServletRequest request,
                        RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        try {
            Order order = orderService.prepareVnPayPayment(orderCode, user);
            return "redirect:" + vnPayService.createPaymentUrl(order, request);
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/payment/" + orderCode;
        }
    }

    // VNPay chuyển trình duyệt của khách về đây sau khi thanh toán.
    // Không yêu cầu đăng nhập vì tin cậy dựa vào chữ ký, không dựa vào cookie.
    @GetMapping("/payment/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        Map<String, String> params = readParams(request);
        VnPayResult result = vnPayService.verifySignature(params)
                ? orderService.applyVnPayResult(params)
                : VnPayResult.INVALID_SIGNATURE;

        model.addAttribute("result", result);
        model.addAttribute("orderCode", params.get("vnp_TxnRef"));
        return "order/vnpay-result";
    }

    // VNPay gọi máy chủ → máy chủ (IPN). Đặt URL này trong trang quản trị merchant của VNPay.
    @GetMapping(value = "/payment/vnpay-ipn", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> vnpayIpn(HttpServletRequest request) {
        Map<String, String> params = readParams(request);
        VnPayResult result = vnPayService.verifySignature(params)
                ? orderService.applyVnPayResult(params)
                : VnPayResult.INVALID_SIGNATURE;

        Map<String, String> response = new HashMap<>();
        switch (result) {
            case PAID, FAILED -> {
                response.put("RspCode", "00");
                response.put("Message", "Confirm Success");
            }
            case ORDER_NOT_FOUND -> {
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
            }
            case ALREADY_CONFIRMED -> {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
            }
            case AMOUNT_MISMATCH -> {
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
            }
            default -> {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
            }
        }
        return response;
    }

    private Map<String, String> readParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (v.length > 0) {
                params.put(k, v[0]);
            }
        });
        return params;
    }
}
