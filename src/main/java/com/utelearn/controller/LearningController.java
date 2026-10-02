package com.utelearn.controller;

import com.utelearn.entity.Course;
import com.utelearn.entity.Enrollment;
import com.utelearn.entity.Lesson;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CourseService;
import com.utelearn.service.EnrollmentService;
import com.utelearn.service.LearningService;
import com.utelearn.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class LearningController {

    private final LearningService learningService;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final UserService userService;

    public LearningController(LearningService learningService, EnrollmentService enrollmentService,
                              CourseService courseService, UserService userService) {
        this.learningService = learningService;
        this.enrollmentService = enrollmentService;
        this.courseService = courseService;
        this.userService = userService;
    }

    // Trang "Khóa học của tôi"
    @GetMapping("/my-courses")
    public String myCourses(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("enrollments", enrollmentService.getMyCourses(user));
        return "learning/my-courses";
    }

    // Trang học bài: /learn/1?lessonId=5
    @GetMapping("/learn/{courseId}")
    public String learn(@PathVariable Long courseId,
                        @RequestParam(required = false) Long lessonId,
                        Principal principal,
                        Model model,
                        RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        Course course = courseService.findById(courseId);
        Enrollment enrollment = enrollmentService.findEnrollment(user, course);
        List<Long> completedIds = learningService.getCompletedLessonIds(enrollment);

        Lesson lesson;
        try {
            lesson = learningService.chooseLesson(course, lessonId, completedIds);
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/courses/" + courseId;
        }

        if (!learningService.canViewLesson(user, course, lesson)) {
            redirect.addFlashAttribute("error", "Bạn cần đăng ký khóa học để xem bài này");
            return "redirect:/courses/" + courseId;
        }

        model.addAttribute("course", course);
        model.addAttribute("lesson", lesson);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("completedIds", completedIds);
        model.addAttribute("nextLesson", learningService.getNextLesson(course, lesson));
        model.addAttribute("prevLesson", learningService.getPreviousLesson(course, lesson));
        return "learning/learn";
    }

    // Bấm "Hoàn thành bài học" -> lưu tiến độ -> chuyển sang bài kế tiếp
    @PostMapping("/learn/{courseId}/lessons/{lessonId}/complete")
    public String completeLesson(@PathVariable Long courseId,
                                 @PathVariable Long lessonId,
                                 Principal principal,
                                 RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        try {
            Enrollment enrollment = learningService.markLessonComplete(user, courseId, lessonId);
            if (enrollment.getProgressPercent() == 100) {
                redirect.addFlashAttribute("success", "Chúc mừng! Bạn đã hoàn thành khóa học 🎉");
            }
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/learn/" + courseId + "?lessonId=" + lessonId;
        }

        Course course = courseService.findById(courseId);
        Lesson next = learningService.getNextLesson(course, learningService.findLesson(lessonId));
        if (next != null) {
            return "redirect:/learn/" + courseId + "?lessonId=" + next.getId();
        }
        return "redirect:/learn/" + courseId + "?lessonId=" + lessonId;
    }
}
