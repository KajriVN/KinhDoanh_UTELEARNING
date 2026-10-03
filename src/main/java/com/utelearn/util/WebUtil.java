package com.utelearn.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

// Hàm tiện ích cho controller
public final class WebUtil {

    private WebUtil() {
    }

    // Quay lại trang người dùng vừa đứng (theo header Referer).
    // Chỉ lấy phần đường dẫn + query, bỏ tên miền -> không thể bị lợi dụng chuyển sang trang lạ.
    public static String redirectBack(HttpServletRequest request, String defaultPath) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "redirect:" + defaultPath;
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return "redirect:" + defaultPath;
            }
            return "redirect:" + path + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
        } catch (IllegalArgumentException e) {
            return "redirect:" + defaultPath;
        }
    }

    // Kiểm tra đường dẫn nội bộ an toàn để chuyển hướng (chỉ nhận "/abc", không nhận "//evil.com" hay "http://...")
    public static boolean isSafeLocalPath(String path) {
        return path != null && path.startsWith("/") && !path.startsWith("//") && !path.startsWith("/\\")
                && !path.contains("\r") && !path.contains("\n");
    }
}
