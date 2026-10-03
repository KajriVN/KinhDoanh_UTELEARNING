package com.utelearn.controller.instructor;

import com.utelearn.dto.LessonForm;
import com.utelearn.entity.Course;
import com.utelearn.entity.Lesson;
import com.utelearn.entity.Section;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CourseService;
import com.utelearn.service.CurriculumService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

// Giảng viên soạn nội dung: thêm/sửa/xóa chương và bài học
@Controller
@RequestMapping("/instructor")
public class InstructorCurriculumController {

    private final CurriculumService curriculumService;
    private final CourseService courseService;
    private final UserService userService;

    public InstructorCurriculumController(CurriculumService curriculumService, CourseService courseService,
                                          UserService userService) {
        this.curriculumService = curriculumService;
        this.courseService = courseService;
        this.userService = userService;
    }

    @GetMapping("/courses/{courseId}/curriculum")
    public String curriculum(@PathVariable Long courseId, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        model.addAttribute("course", courseService.getOwnedCourse(courseId, me));
        return "instructor/curriculum";
    }

    // ================== CHƯƠNG ==================

    @PostMapping("/courses/{courseId}/sections")
    public String addSection(@PathVariable Long courseId, @RequestParam String title,
                             Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        try {
            curriculumService.addSection(courseId, title, me);
            redirect.addFlashAttribute("success", "Đã thêm chương mới");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/courses/" + courseId + "/curriculum";
    }

    @PostMapping("/sections/{sectionId}/rename")
    public String renameSection(@PathVariable Long sectionId, @RequestParam String title,
                                Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Section section = curriculumService.getOwnedSection(sectionId, me);
        try {
            curriculumService.renameSection(sectionId, title, me);
            redirect.addFlashAttribute("success", "Đã đổi tên chương");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/courses/" + section.getCourse().getId() + "/curriculum";
    }

    @PostMapping("/sections/{sectionId}/delete")
    public String deleteSection(@PathVariable Long sectionId, Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Long courseId = curriculumService.deleteSection(sectionId, me);
        redirect.addFlashAttribute("success", "Đã xóa chương");
        return "redirect:/instructor/courses/" + courseId + "/curriculum";
    }

    @PostMapping("/sections/{sectionId}/move")
    public String moveSection(@PathVariable Long sectionId, @RequestParam String dir, Principal principal) {
        User me = userService.findByEmail(principal.getName());
        Long courseId = curriculumService.moveSection(sectionId, dir, me);
        return "redirect:/instructor/courses/" + courseId + "/curriculum";
    }

    @PostMapping("/lessons/{lessonId}/move")
    public String moveLesson(@PathVariable Long lessonId, @RequestParam String dir, Principal principal) {
        User me = userService.findByEmail(principal.getName());
        Long courseId = curriculumService.moveLesson(lessonId, dir, me);
        return "redirect:/instructor/courses/" + courseId + "/curriculum";
    }

    // ================== BÀI HỌC ==================

    @GetMapping("/sections/{sectionId}/lessons/new")
    public String newLessonForm(@PathVariable Long sectionId, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Section section = curriculumService.getOwnedSection(sectionId, me);
        model.addAttribute("form", new LessonForm());
        model.addAttribute("section", section);
        model.addAttribute("course", section.getCourse());
        return "instructor/lesson-form";
    }

    @PostMapping("/sections/{sectionId}/lessons/new")
    public String createLesson(@PathVariable Long sectionId,
                               @Valid @ModelAttribute("form") LessonForm form,
                               BindingResult result,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Section section = curriculumService.getOwnedSection(sectionId, me);
        if (result.hasErrors()) {
            model.addAttribute("section", section);
            model.addAttribute("course", section.getCourse());
            return "instructor/lesson-form";
        }
        try {
            curriculumService.addLesson(sectionId, form, me);
            redirect.addFlashAttribute("success", "Đã thêm bài học");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/courses/" + section.getCourse().getId() + "/curriculum";
    }

    @GetMapping("/lessons/{lessonId}/edit")
    public String editLessonForm(@PathVariable Long lessonId, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Lesson lesson = curriculumService.getOwnedLesson(lessonId, me);
        model.addAttribute("form", LessonForm.fromEntity(lesson));
        model.addAttribute("section", lesson.getSection());
        model.addAttribute("course", lesson.getSection().getCourse());
        return "instructor/lesson-form";
    }

    @PostMapping("/lessons/{lessonId}/edit")
    public String updateLesson(@PathVariable Long lessonId,
                               @Valid @ModelAttribute("form") LessonForm form,
                               BindingResult result,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Lesson lesson = curriculumService.getOwnedLesson(lessonId, me);
        Course course = lesson.getSection().getCourse();
        if (result.hasErrors()) {
            model.addAttribute("section", lesson.getSection());
            model.addAttribute("course", course);
            return "instructor/lesson-form";
        }
        try {
            curriculumService.updateLesson(lessonId, form, me);
            redirect.addFlashAttribute("success", "Đã cập nhật bài học");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/courses/" + course.getId() + "/curriculum";
    }

    @PostMapping("/lessons/{lessonId}/delete")
    public String deleteLesson(@PathVariable Long lessonId, Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Long courseId = curriculumService.deleteLesson(lessonId, me);
        redirect.addFlashAttribute("success", "Đã xóa bài học");
        return "redirect:/instructor/courses/" + courseId + "/curriculum";
    }
}
