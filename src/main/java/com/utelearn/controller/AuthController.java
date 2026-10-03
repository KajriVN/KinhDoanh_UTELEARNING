package com.utelearn.controller;

import com.utelearn.dto.RegisterForm;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.security.JwtUtil;
import com.utelearn.service.PasswordResetService;
import com.utelearn.service.UserService;
import com.utelearn.util.WebUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService,
                          PasswordResetService passwordResetService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    // ================== ĐĂNG NHẬP ==================

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("redirect", WebUtil.isSafeLocalPath(redirect) ? redirect : null);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        @RequestParam(required = false) String redirect,
                        HttpServletResponse response,
                        Model model) {
        email = email.trim().toLowerCase();
        // chỉ chấp nhận đường dẫn nội bộ, tránh bị lợi dụng chuyển sang trang lạ (open redirect)
        String backTo = WebUtil.isSafeLocalPath(redirect) ? redirect : null;
        model.addAttribute("redirect", backTo);
        try {
            // Spring Security kiểm tra email + mật khẩu (BCrypt)
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (DisabledException e) {
            model.addAttribute("error", "Tài khoản của bạn đã bị khóa");
            model.addAttribute("email", email);
            return "auth/login";
        } catch (BadCredentialsException e) {
            model.addAttribute("error", "Email hoặc mật khẩu không đúng");
            model.addAttribute("email", email);
            return "auth/login";
        }

        User user = userService.findByEmail(email);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        // lưu JWT vào cookie HttpOnly (JavaScript không đọc được -> chống XSS lấy token)
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .path("/")
                .maxAge(jwtUtil.getExpirationSeconds())
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // bị chặn ở trang nào thì đăng nhập xong quay lại đúng trang đó
        if (backTo != null) {
            return "redirect:" + backTo;
        }
        // không thì chuyển trang theo vai trò
        switch (user.getRole()) {
            case ADMIN:
                return "redirect:/admin";
            case INSTRUCTOR:
                return "redirect:/instructor/courses";
            default:
                return "redirect:/";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        // xóa cookie bằng cách set maxAge = 0
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return "redirect:/";
    }

    // ================== ĐĂNG KÝ ==================

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
        redirect.addFlashAttribute("success", "Đăng ký thành công, mời bạn đăng nhập");
        return "redirect:/auth/login";
    }

    // ================== QUÊN MẬT KHẨU ==================

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    // luôn trả lời giống nhau dù email có tồn tại hay không
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, RedirectAttributes redirect) {
        passwordResetService.requestReset(email);
        redirect.addFlashAttribute("success",
                "Nếu email này đã đăng ký, chúng tôi đã gửi liên kết đặt lại mật khẩu. Liên kết có hiệu lực 15 phút.");
        return "redirect:/auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        try {
            passwordResetService.validate(token);
            model.addAttribute("valid", true);
        } catch (BusinessException e) {
            model.addAttribute("valid", false);
            model.addAttribute("error", e.getMessage());
        }
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                Model model,
                                RedirectAttributes redirect) {
        try {
            passwordResetService.resetPassword(token, password, confirmPassword);
        } catch (BusinessException e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            // vẫn còn dùng được token (lỗi nhập liệu) thì hiện lại form, hết hạn thì ẩn form
            boolean stillValid;
            try {
                passwordResetService.validate(token);
                stillValid = true;
            } catch (BusinessException ex) {
                stillValid = false;
            }
            model.addAttribute("valid", stillValid);
            return "auth/reset-password";
        }
        redirect.addFlashAttribute("success", "Đã đổi mật khẩu, mời bạn đăng nhập");
        return "redirect:/auth/login";
    }
}
