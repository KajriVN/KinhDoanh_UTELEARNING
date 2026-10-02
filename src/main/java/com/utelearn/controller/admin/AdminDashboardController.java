package com.utelearn.controller.admin;

import com.utelearn.entity.CourseStatus;
import com.utelearn.entity.Role;
import com.utelearn.service.CourseService;
import com.utelearn.service.OrderService;
import com.utelearn.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final UserService userService;
    private final CourseService courseService;
    private final OrderService orderService;

    public AdminDashboardController(UserService userService, CourseService courseService, OrderService orderService) {
        this.userService = userService;
        this.courseService = courseService;
        this.orderService = orderService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("studentCount", userService.countByRole(Role.STUDENT));
        model.addAttribute("instructorCount", userService.countByRole(Role.INSTRUCTOR));
        model.addAttribute("publishedCount", courseService.countByStatus(CourseStatus.PUBLISHED));
        model.addAttribute("pendingCount", courseService.countByStatus(CourseStatus.PENDING));
        model.addAttribute("paidOrderCount", orderService.countPaidOrders());
        model.addAttribute("revenue", orderService.getTotalRevenue());
        model.addAttribute("pendingCourses", courseService.findPendingCourses());
        // TODO (hand-off): biểu đồ doanh thu theo tháng bằng Chart.js
        return "admin/dashboard";
    }
}
