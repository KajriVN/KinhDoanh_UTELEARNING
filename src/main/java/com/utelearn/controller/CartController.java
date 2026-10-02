package com.utelearn.controller;

import com.utelearn.entity.CartItem;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CartService;
import com.utelearn.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping
    public String viewCart(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        List<CartItem> items = cartService.getItems(user);
        model.addAttribute("items", items);
        model.addAttribute("total", cartService.getTotal(items));
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
