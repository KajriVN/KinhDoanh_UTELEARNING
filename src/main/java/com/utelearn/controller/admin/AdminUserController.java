package com.utelearn.controller.admin;

import com.utelearn.entity.Role;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
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

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Role role,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("userPage", userService.search(keyword, role, Math.max(page, 0)));
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    // Khóa / mở khóa tài khoản
    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        User admin = userService.findByEmail(principal.getName());
        try {
            userService.toggleEnabled(id, admin);
            redirect.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // Đổi vai trò (vd: cấp quyền giảng viên)
    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role,
                             Principal principal, RedirectAttributes redirect) {
        User admin = userService.findByEmail(principal.getName());
        try {
            userService.changeRole(id, role, admin);
            redirect.addFlashAttribute("success", "Đã đổi vai trò thành " + role.getLabel());
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
