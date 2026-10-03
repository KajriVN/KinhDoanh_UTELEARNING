package com.utelearn.controller.instructor;

import com.utelearn.dto.AssignmentForm;
import com.utelearn.entity.Assignment;
import com.utelearn.entity.Course;
import com.utelearn.entity.Submission;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.AssignmentService;
import com.utelearn.service.CourseService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Giảng viên giao bài tập + chấm điểm bài nộp
@Controller
@RequestMapping("/instructor")
public class InstructorAssignmentController {

    private final AssignmentService assignmentService;
    private final CourseService courseService;
    private final UserService userService;

    public InstructorAssignmentController(AssignmentService assignmentService, CourseService courseService,
                                          UserService userService) {
        this.assignmentService = assignmentService;
        this.courseService = courseService;
        this.userService = userService;
    }

    // Tổng quan: các khóa của tôi + số bài tập + số bài chưa chấm
    @GetMapping("/assignments")
    public String overview(Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        List<Course> courses = courseService.findByInstructor(me);
        Map<Long, Long> assignmentCounts = new HashMap<>();
        Map<Long, Long> ungradedCounts = new HashMap<>();
        for (Course c : courses) {
            assignmentCounts.put(c.getId(), assignmentService.countByCourse(c));
            ungradedCounts.put(c.getId(), assignmentService.countUngraded(c));
        }
        model.addAttribute("courses", courses);
        model.addAttribute("assignmentCounts", assignmentCounts);
        model.addAttribute("ungradedCounts", ungradedCounts);
        return "instructor/assignment-overview";
    }

    @GetMapping("/courses/{courseId}/assignments")
    public String list(@PathVariable Long courseId, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Course course = courseService.getOwnedCourse(courseId, me);
        List<Assignment> assignments = assignmentService.findByCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("assignments", assignments);
        model.addAttribute("submissionCounts", assignmentService.countSubmissions(assignments));
        return "instructor/assignments";
    }

    @GetMapping("/courses/{courseId}/assignments/new")
    public String newForm(@PathVariable Long courseId, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        model.addAttribute("course", courseService.getOwnedCourse(courseId, me));
        model.addAttribute("form", new AssignmentForm());
        return "instructor/assignment-form";
    }

    @PostMapping("/courses/{courseId}/assignments/new")
    public String create(@PathVariable Long courseId,
                         @Valid @ModelAttribute("form") AssignmentForm form,
                         BindingResult result,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Course course = courseService.getOwnedCourse(courseId, me);
        if (result.hasErrors()) {
            model.addAttribute("course", course);
            return "instructor/assignment-form";
        }
        try {
            assignmentService.create(courseId, form, me);
            redirect.addFlashAttribute("success", "Đã giao bài tập");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/courses/" + courseId + "/assignments";
    }

    @GetMapping("/assignments/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Assignment assignment = assignmentService.getOwnedAssignment(id, me);
        model.addAttribute("course", assignment.getCourse());
        model.addAttribute("assignment", assignment);
        model.addAttribute("form", AssignmentForm.fromEntity(assignment));
        return "instructor/assignment-form";
    }

    @PostMapping("/assignments/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") AssignmentForm form,
                         BindingResult result,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Assignment assignment = assignmentService.getOwnedAssignment(id, me);
        Long courseId = assignment.getCourse().getId();
        if (result.hasErrors()) {
            model.addAttribute("course", assignment.getCourse());
            model.addAttribute("assignment", assignment);
            return "instructor/assignment-form";
        }
        try {
            assignmentService.update(id, form, me);
            redirect.addFlashAttribute("success", "Đã cập nhật bài tập");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/courses/" + courseId + "/assignments";
    }

    @PostMapping("/assignments/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Long courseId = assignmentService.delete(id, me);
        redirect.addFlashAttribute("success", "Đã xóa bài tập");
        return "redirect:/instructor/courses/" + courseId + "/assignments";
    }

    // Danh sách bài nộp + ô chấm điểm
    @GetMapping("/assignments/{id}/submissions")
    public String submissions(@PathVariable Long id, Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Assignment assignment = assignmentService.getOwnedAssignment(id, me);
        model.addAttribute("assignment", assignment);
        model.addAttribute("course", assignment.getCourse());
        model.addAttribute("submissions", assignmentService.getSubmissions(assignment));
        return "instructor/submissions";
    }

    @PostMapping("/submissions/{id}/grade")
    public String grade(@PathVariable Long id,
                        @RequestParam(required = false) Double score,
                        @RequestParam(required = false) String feedback,
                        Principal principal,
                        RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Submission submission = assignmentService.getOwnedSubmission(id, me);
        try {
            assignmentService.grade(id, score, feedback, me);
            redirect.addFlashAttribute("success", "Đã chấm điểm bài của " + submission.getStudent().getFullName());
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/assignments/" + submission.getAssignment().getId() + "/submissions";
    }
}
