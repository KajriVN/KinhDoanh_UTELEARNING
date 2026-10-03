package com.utelearn.security;

import com.utelearn.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// Chưa đăng nhập mà vào trang cần quyền:
// - trang web  -> chuyển về /auth/login?redirect=<trang đang vào> để đăng nhập xong quay lại đúng chỗ
// - REST API   -> trả 401 JSON (app mobile không cần trang login)
@Component
public class LoginEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Chưa đăng nhập hoặc token không hợp lệ\"}");
            return;
        }

        String target = "/auth/login";
        // chỉ ghi nhớ trang khi là GET (POST không thể "quay lại" bằng redirect)
        if ("GET".equals(request.getMethod())) {
            String back = path + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
            if (WebUtil.isSafeLocalPath(back) && !back.startsWith("/auth/")) {
                target += "?redirect=" + URLEncoder.encode(back, StandardCharsets.UTF_8);
            }
        }
        response.sendRedirect(request.getContextPath() + target);
    }
}
