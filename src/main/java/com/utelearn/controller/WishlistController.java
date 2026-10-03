package com.utelearn.controller;

import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.UserService;
import com.utelearn.service.WishlistService;
import com.utelearn.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserService userService;

    public WishlistController(WishlistService wishlistService, UserService userService) {
        this.wishlistService = wishlistService;
        this.userService = userService;
    }

    @GetMapping("/wishlist")
    public String list(Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        model.addAttribute("wishlist", wishlistService.findByUser(me));
        return "wishlist/list";
    }

    // bấm trái tim ở thẻ khóa học / trang chi tiết -> bật/tắt rồi quay lại đúng trang đang xem
    @PostMapping("/wishlist/toggle/{courseId}")
    public String toggle(@PathVariable Long courseId, Principal principal, HttpServletRequest request,
                         RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        try {
            boolean liked = wishlistService.toggle(me, courseId);
            redirect.addFlashAttribute("success", liked ? "Đã thêm vào danh sách yêu thích" : "Đã bỏ khỏi danh sách yêu thích");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return WebUtil.redirectBack(request, "/wishlist");
    }
}
