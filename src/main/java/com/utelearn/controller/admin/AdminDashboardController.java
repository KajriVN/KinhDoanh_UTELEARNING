package com.utelearn.controller.admin;

import com.utelearn.entity.CourseStatus;
import com.utelearn.entity.Role;
import com.utelearn.service.CourseService;
import com.utelearn.service.OrderService;
import com.utelearn.service.StatsService;
import com.utelearn.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class AdminDashboardController {

    private final UserService userService;
    private final CourseService courseService;
    private final OrderService orderService;
    private final StatsService statsService;

    public AdminDashboardController(UserService userService, CourseService courseService, OrderService orderService,
                                    StatsService statsService) {
        this.userService = userService;
        this.courseService = courseService;
        this.orderService = orderService;
        this.statsService = statsService;
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

        // dữ liệu cho 3 biểu đồ Chart.js
        Map<String, BigDecimal> monthly = statsService.revenueLast12Months();
        model.addAttribute("monthLabels", new ArrayList<>(monthly.keySet()));
        model.addAttribute("monthValues", new ArrayList<>(monthly.values()));
        List<StatsService.NamedValue> top = statsService.topCourses(5);
        model.addAttribute("topLabels", top.stream().map(StatsService.NamedValue::name).toList());
        model.addAttribute("topValues", top.stream().map(StatsService.NamedValue::value).toList());
        List<StatsService.NamedValue> byCategory = statsService.publishedCoursesByCategory();
        model.addAttribute("categoryLabels", byCategory.stream().map(StatsService.NamedValue::name).toList());
        model.addAttribute("categoryValues", byCategory.stream().map(StatsService.NamedValue::value).toList());
        return "admin/dashboard";
    }
}
