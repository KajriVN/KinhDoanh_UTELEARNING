package com.utelearn.controller;

import com.utelearn.entity.Notification;
import com.utelearn.entity.User;
import com.utelearn.service.NotificationService;
import com.utelearn.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

// Trang danh sách thông báo (dropdown chuông nằm trong navbar)
@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/notifications")
    public String list(@RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        model.addAttribute("notificationPage", notificationService.getPage(me, page));
        return "notification/list";
    }

    // bấm vào 1 thông báo -> đánh dấu đã đọc -> chuyển tới trang liên quan
    @GetMapping("/notifications/{id}/go")
    public String go(@PathVariable Long id, Principal principal) {
        User me = userService.findByEmail(principal.getName());
        Notification n = notificationService.open(id, me);
        String link = n.getLink();
        // chỉ chuyển trong nội bộ site (bắt đầu bằng 1 dấu /)
        if (link != null && link.startsWith("/") && !link.startsWith("//")) {
            return "redirect:" + link;
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAll(Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        notificationService.markAllRead(me);
        redirect.addFlashAttribute("success", "Đã đánh dấu tất cả là đã đọc");
        return "redirect:/notifications";
    }
}
