package com.utelearn.controller;

import com.utelearn.entity.Course;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CartService;
import com.utelearn.service.CategoryService;
import com.utelearn.service.CourseService;
import com.utelearn.service.EnrollmentService;
import com.utelearn.service.ReviewService;
import com.utelearn.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class CourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final EnrollmentService enrollmentService;
    private final CartService cartService;
    private final UserService userService;
    private final ReviewService reviewService;

    public CourseController(CourseService courseService, CategoryService categoryService,
                            EnrollmentService enrollmentService, CartService cartService,
                            UserService userService, ReviewService reviewService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
        this.enrollmentService = enrollmentService;
        this.cartService = cartService;
        this.userService = userService;
        this.reviewService = reviewService;
    }

    // Danh sách + tìm kiếm + lọc + phân trang
    @GetMapping("/courses")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String price,
                       @RequestParam(defaultValue = "newest") String sort,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Course> coursePage = courseService.searchPublished(keyword, categoryId, price, sort, page);

        model.addAttribute("coursePage", coursePage);
        model.addAttribute("categories", categoryService.findAll());
        // giữ lại giá trị filter trên form
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("price", price);
        model.addAttribute("sort", sort);
        return "course/list";
    }

    // Chi tiết khóa học
    @GetMapping("/courses/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        Course course = courseService.getCourseForDetail(id, user);

        model.addAttribute("course", course);
        model.addAttribute("studentCount", courseService.countStudents(course));
        model.addAttribute("enrolled", enrollmentService.isEnrolled(user, course));
        model.addAttribute("inCart", cartService.isInCart(user, course));
        model.addAttribute("canManage", courseService.canManage(course, user));
        model.addAttribute("reviews", reviewService.findByCourse(course));
        model.addAttribute("myReview", reviewService.findMine(user, course));
        return "course/detail";
    }

    // Gửi / sửa đánh giá của mình
    @PostMapping("/courses/{id}/reviews")
    public String saveReview(@PathVariable Long id,
                             @RequestParam int rating,
                             @RequestParam(required = false) String comment,
                             Principal principal, RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        try {
            reviewService.save(user, id, rating, comment);
            redirect.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá khóa học");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/courses/" + id + "#reviews";
    }

    @PostMapping("/courses/{id}/reviews/delete")
    public String deleteReview(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        reviewService.deleteMine(user, id);
        redirect.addFlashAttribute("success", "Đã xóa đánh giá của bạn");
        return "redirect:/courses/" + id + "#reviews";
    }

    // Đăng ký khóa học miễn phí (không cần qua giỏ hàng)
    @PostMapping("/courses/{id}/enroll-free")
    public String enrollFree(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        try {
            enrollmentService.enrollFreeCourse(user, id);
            redirect.addFlashAttribute("success", "Đăng ký thành công, bắt đầu học thôi!");
            return "redirect:/learn/" + id;
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/courses/" + id;
        }
    }
}
