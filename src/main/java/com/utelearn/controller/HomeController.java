package com.utelearn.controller;

import com.utelearn.service.CategoryService;
import com.utelearn.service.CourseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CourseService courseService;
    private final CategoryService categoryService;

    public HomeController(CourseService courseService, CategoryService categoryService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("latestCourses", courseService.getLatestCourses());
        model.addAttribute("categories", categoryService.findAll());
        return "home/index";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}
