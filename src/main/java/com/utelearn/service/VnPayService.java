package com.utelearn.service;

import com.utelearn.entity.Order;
import com.utelearn.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// Cổng thanh toán VNPay (sandbox): dựng link thanh toán có chữ ký HMAC-SHA512 và kiểm tra chữ ký khi VNPay trả về.
// Tài liệu: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
@Service
public class VnPayService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Value("${vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vnpay.pay-url}")
    private String payUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    // chưa điền tmn-code / hash-secret thì ẩn nút VNPay
    public boolean isEnabled() {
        return tmnCode != null && !tmnCode.isBlank() && hashSecret != null && !hashSecret.isBlank();
    }

    public String createPaymentUrl(Order order, HttpServletRequest request) {
        if (!isEnabled()) {
            throw new BusinessException("VNPay chưa được cấu hình (thiếu VNPAY_TMN_CODE / VNPAY_HASH_SECRET)");
        }
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        // VNPay yêu cầu số tiền nhân 100 (không có phần thập phân)
        params.put("vnp_Amount", String.valueOf(toVnpAmount(order)));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", order.getOrderCode());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp(request));
        params.put("vnp_CreateDate", now.format(DATE_FORMAT));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(DATE_FORMAT));

        // key sắp xếp A->Z, value URL-encode; chuỗi này vừa dùng để ký vừa là query của link
        String query = buildQuery(params);
        return payUrl + "?" + query + "&vnp_SecureHash=" + sign(query);
    }

    public static long toVnpAmount(Order order) {
        return order.getFinalAmount().longValueExact() * 100;
    }

    // Kiểm tra chữ ký của dữ liệu VNPay gửi về (bỏ vnp_SecureHash và vnp_SecureHashType khi tính)
    public boolean verifySignature(Map<String, String> params) {
        if (!isEnabled()) {
            return false;
        }
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isBlank()) {
            return false;
        }
        Map<String, String> data = new HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey().startsWith("vnp_") && !e.getKey().equals("vnp_SecureHash")
                    && !e.getKey().equals("vnp_SecureHashType") && e.getValue() != null && !e.getValue().isEmpty()) {
                data.put(e.getKey(), e.getValue());
            }
        }
        String expected = sign(buildQuery(data));
        // so sánh thời gian cố định, không phân biệt hoa thường
        return MessageDigest.isEqual(expected.toLowerCase().getBytes(StandardCharsets.UTF_8),
                received.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private String buildQuery(Map<String, String> params) {
        List<String> names = new ArrayList<>(params.keySet());
        Collections.sort(names);
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            String value = params.get(name);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(name, StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không ký được dữ liệu VNPay", e);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        // localhost IPv6 -> IPv4 cho VNPay chấp nhận
        if (ip == null || ip.contains(":")) {
            return "127.0.0.1";
        }
        return ip;
    }
}
