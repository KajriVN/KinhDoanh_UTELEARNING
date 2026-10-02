package com.utelearn.controller.admin;

import com.utelearn.entity.CourseStatus;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CourseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Admin duyệt khóa học
@Controller
@RequestMapping("/admin/courses")
public class AdminCourseController {

    private final CourseService courseService;

    public AdminCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) CourseStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("coursePage", courseService.findAllForAdmin(status, page));
        model.addAttribute("status", status);
        model.addAttribute("statuses", CourseStatus.values());
        model.addAttribute("pendingCount", courseService.countByStatus(CourseStatus.PENDING));
        return "admin/courses";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            courseService.approve(id);
            redirect.addFlashAttribute("success", "Đã duyệt khóa học");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses?status=PENDING";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam String reason, RedirectAttributes redirect) {
        try {
            courseService.reject(id, reason);
            redirect.addFlashAttribute("success", "Đã từ chối khóa học");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses?status=PENDING";
    }
}
