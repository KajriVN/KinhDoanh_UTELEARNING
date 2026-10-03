package com.utelearn.controller.instructor;

import com.utelearn.entity.User;
import com.utelearn.service.StatsService;
import com.utelearn.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Giảng viên xem doanh thu các khóa của mình (tính theo giá bán trong đơn đã thanh toán)
@Controller
public class InstructorRevenueController {

    private final StatsService statsService;
    private final UserService userService;

    public InstructorRevenueController(StatsService statsService, UserService userService) {
        this.statsService = statsService;
        this.userService = userService;
    }

    @GetMapping("/instructor/revenue")
    public String revenue(Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Map<String, BigDecimal> monthly = statsService.instructorRevenueLast12Months(me);
        List<StatsService.CourseRevenue> byCourse = statsService.instructorRevenueByCourse(me);

        model.addAttribute("total", statsService.instructorTotal(me));
        model.addAttribute("monthLabels", new ArrayList<>(monthly.keySet()));
        model.addAttribute("monthValues", new ArrayList<>(monthly.values()));
        model.addAttribute("monthly", monthly);
        model.addAttribute("byCourse", byCourse);
        model.addAttribute("totalSold", byCourse.stream().mapToLong(StatsService.CourseRevenue::sold).sum());
        return "instructor/revenue";
    }
}
