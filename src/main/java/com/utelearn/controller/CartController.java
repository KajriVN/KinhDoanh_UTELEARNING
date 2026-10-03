package com.utelearn.controller;

import com.utelearn.entity.CartItem;
import com.utelearn.entity.Coupon;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CartService;
import com.utelearn.service.CouponService;
import com.utelearn.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;
    private final CouponService couponService;

    public CartController(CartService cartService, UserService userService, CouponService couponService) {
        this.cartService = cartService;
        this.userService = userService;
        this.couponService = couponService;
    }

    @GetMapping
    public String viewCart(@RequestParam(required = false) String couponCode, Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        List<CartItem> items = cartService.getItems(user);
        BigDecimal total = cartService.getTotal(items);
        BigDecimal discount = BigDecimal.ZERO;
        // có nhập mã thì tính thử số tiền giảm, mã sai thì báo lỗi tại ô nhập
        if (couponCode != null && !couponCode.isBlank()) {
            try {
                Coupon coupon = couponService.validate(couponCode);
                discount = couponService.calculateDiscount(coupon, total);
                model.addAttribute("coupon", coupon);
            } catch (BusinessException e) {
                model.addAttribute("couponError", e.getMessage());
            }
        }
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        model.addAttribute("discount", discount);
        model.addAttribute("finalTotal", total.subtract(discount));
        return "cart/cart";
    }

    // buyNow = true -> thêm xong chuyển thẳng tới giỏ hàng
    @PostMapping("/add/{courseId}")
    public String add(@PathVariable Long courseId,
                      @RequestParam(defaultValue = "false") boolean buyNow,
                      Principal principal,
                      RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        try {
            cartService.addToCart(user, courseId);
            redirect.addFlashAttribute("success", "Đã thêm khóa học vào giỏ hàng");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/courses/" + courseId;
        }
        return buyNow ? "redirect:/cart" : "redirect:/courses/" + courseId;
    }

    @PostMapping("/remove/{courseId}")
    public String remove(@PathVariable Long courseId, Principal principal, RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        cartService.removeFromCart(user, courseId);
        redirect.addFlashAttribute("success", "Đã xóa khỏi giỏ hàng");
        return "redirect:/cart";
    }
}
