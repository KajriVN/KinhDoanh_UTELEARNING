package com.utelearn.controller.instructor;

import com.utelearn.dto.CourseForm;
import com.utelearn.entity.Course;
import com.utelearn.entity.CourseLevel;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CategoryService;
import com.utelearn.service.CourseService;
import com.utelearn.service.EnrollmentService;
import com.utelearn.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

// Giảng viên quản lý khóa học của mình
@Controller
@RequestMapping("/instructor/courses")
public class InstructorCourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    public InstructorCourseController(CourseService courseService, CategoryService categoryService,
                                      EnrollmentService enrollmentService, UserService userService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    @GetMapping
    public String myCourses(Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        model.addAttribute("courses", courseService.findByInstructor(me));
        return "instructor/courses";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        CourseForm form = new CourseForm();
        form.setLevel(CourseLevel.BEGINNER);
        model.addAttribute("form", form);
        addFormData(model);
        return "instructor/course-form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") CourseForm form,
                         BindingResult result,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            addFormData(model);
            return "instructor/course-form";
        }
        User me = userService.findByEmail(principal.getName());
        try {
            Course course = courseService.createCourse(form, me);
            redirect.addFlashAttribute("success", "Tạo khóa học thành công, hãy thêm chương và bài học");
            return "redirect:/instructor/courses/" + course.getId() + "/curriculum";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            addFormData(model);
            return "instructor/course-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Course course = courseService.getOwnedCourse(id, me);
        model.addAttribute("form", CourseForm.fromEntity(course));
        model.addAttribute("course", course);
        addFormData(model);
        return "instructor/course-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CourseForm form,
                         BindingResult result,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Course course = courseService.getOwnedCourse(id, me);
        if (result.hasErrors()) {
            form.setCurrentThumbnailUrl(course.getThumbnailUrl());
            model.addAttribute("course", course);
            addFormData(model);
            return "instructor/course-form";
        }
        try {
            courseService.updateCourse(id, form, me);
            redirect.addFlashAttribute("success", "Đã lưu thay đổi");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/courses/" + id + "/edit";
    }

    // Gửi admin duyệt
    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        try {
            courseService.submitForReview(id, me);
            redirect.addFlashAttribute("success", "Đã gửi khóa học cho admin duyệt");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/courses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        try {
            courseService.deleteCourse(id, me);
            redirect.addFlashAttribute("success", "Đã xóa khóa học");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/courses";
    }

    // Danh sách học viên của khóa + tiến độ
    @GetMapping("/{id}/students")
    public String students(@PathVariable Long id, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Course course = courseService.getOwnedCourse(id, me);
        model.addAttribute("course", course);
        model.addAttribute("enrollments", enrollmentService.getStudentsOfCourse(course));
        return "instructor/students";
    }

    private void addFormData(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("levels", CourseLevel.values());
    }
}
