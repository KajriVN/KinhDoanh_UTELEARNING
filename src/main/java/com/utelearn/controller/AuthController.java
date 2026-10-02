package com.utelearn.controller;

import com.utelearn.dto.RegisterForm;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.security.JwtUtil;
import com.utelearn.service.UserService;
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

    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    // ================== ĐĂNG NHẬP ==================

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletResponse response,
                        Model model) {
        email = email.trim().toLowerCase();
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

        // đăng nhập xong chuyển trang theo vai trò
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

    // TODO (hand-off): /auth/forgot-password + /auth/reset-password (gửi OTP qua email)
}
