package com.utelearn.controller;

import com.utelearn.entity.User;
import com.utelearn.service.CartService;
import com.utelearn.service.NotificationService;
import com.utelearn.service.UserService;
import com.utelearn.service.WishlistService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

// Thêm sẵn biến currentUser + cartCount + thông báo vào mọi trang (navbar cần dùng)
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final WishlistService wishlistService;

    public GlobalModelAdvice(UserService userService, CartService cartService,
                             NotificationService notificationService, WishlistService wishlistService) {
        this.userService = userService;
        this.cartService = cartService;
        this.notificationService = notificationService;
        this.wishlistService = wishlistService;
    }

    @ModelAttribute
    public void addCommonAttributes(Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        model.addAttribute("currentUser", user);
        model.addAttribute("cartCount", cartService.count(user));
        // id các khóa đã thích -> tô đỏ trái tim ở thẻ khóa học
        model.addAttribute("wishlistIds", wishlistService.getCourseIds(user));
        // chuông thông báo trên navbar (khách chưa đăng nhập thì không có)
        if (user != null) {
            model.addAttribute("unreadCount", notificationService.countUnread(user));
            model.addAttribute("latestNotifications", notificationService.getLatest(user));
        }
    }
}
