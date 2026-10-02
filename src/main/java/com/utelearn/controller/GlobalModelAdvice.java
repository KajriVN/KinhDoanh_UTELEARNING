package com.utelearn.controller;

import com.utelearn.entity.User;
import com.utelearn.service.CartService;
import com.utelearn.service.UserService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

// Thêm sẵn biến currentUser + cartCount vào mọi trang (navbar cần dùng)
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;
    private final CartService cartService;

    public GlobalModelAdvice(UserService userService, CartService cartService) {
        this.userService = userService;
        this.cartService = cartService;
    }

    @ModelAttribute
    public void addCommonAttributes(Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        model.addAttribute("currentUser", user);
        model.addAttribute("cartCount", cartService.count(user));
    }
}
