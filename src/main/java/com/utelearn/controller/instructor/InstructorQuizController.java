package com.utelearn.controller.instructor;

import com.utelearn.entity.Quiz;
import com.utelearn.entity.QuizQuestion;
import com.utelearn.entity.Section;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CurriculumService;
import com.utelearn.service.QuizService;
import com.utelearn.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

// Giảng viên soạn quiz trắc nghiệm cuối chương
@Controller
@RequestMapping("/instructor")
public class InstructorQuizController {

    private final QuizService quizService;
    private final CurriculumService curriculumService;
    private final UserService userService;

    public InstructorQuizController(QuizService quizService, CurriculumService curriculumService,
                                    UserService userService) {
        this.quizService = quizService;
        this.curriculumService = curriculumService;
        this.userService = userService;
    }

    // Trang quản lý quiz của 1 chương. ?editQuestion=id để sửa câu hỏi.
    @GetMapping("/sections/{sectionId}/quiz")
    public String manage(@PathVariable Long sectionId,
                         @RequestParam(required = false) Long editQuestion,
                         Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        Section section = curriculumService.getOwnedSection(sectionId, me);
        Quiz quiz = quizService.findBySection(section);

        QuizQuestion editing = new QuizQuestion();
        if (quiz != null && editQuestion != null) {
            editing = quiz.getQuestions().stream().filter(q -> q.getId().equals(editQuestion))
                    .findFirst().orElse(editing);
        }
        model.addAttribute("section", section);
        model.addAttribute("course", section.getCourse());
        model.addAttribute("quiz", quiz);
        model.addAttribute("editing", editing);
        if (quiz != null) {
            model.addAttribute("attempts", quizService.recentAttempts(quiz));
        }
        return "instructor/quiz";
    }

    @PostMapping("/sections/{sectionId}/quiz")
    public String saveQuiz(@PathVariable Long sectionId,
                           @RequestParam String title,
                           @RequestParam(defaultValue = "70") int passPercent,
                           Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        try {
            quizService.saveQuiz(sectionId, title, passPercent, me);
            redirect.addFlashAttribute("success", "Đã lưu bài kiểm tra");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/sections/" + sectionId + "/quiz";
    }

    @PostMapping("/quizzes/{quizId}/delete")
    public String deleteQuiz(@PathVariable Long quizId, Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Long sectionId = quizService.deleteQuiz(quizId, me);
        redirect.addFlashAttribute("success", "Đã xóa bài kiểm tra");
        return "redirect:/instructor/sections/" + sectionId + "/quiz";
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public String saveQuestion(@PathVariable Long quizId,
                               @RequestParam(required = false) Long questionId,
                               @RequestParam String content,
                               @RequestParam String optionA,
                               @RequestParam String optionB,
                               @RequestParam String optionC,
                               @RequestParam String optionD,
                               @RequestParam(defaultValue = "0") int correct,
                               Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Long sectionId = quizService.getOwnedQuiz(quizId, me).getSection().getId();
        try {
            quizService.saveQuestion(quizId, questionId, content, optionA, optionB, optionC, optionD, correct, me);
            redirect.addFlashAttribute("success", questionId == null ? "Đã thêm câu hỏi" : "Đã cập nhật câu hỏi");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/instructor/sections/" + sectionId + "/quiz";
    }

    @PostMapping("/questions/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long questionId, Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Long quizId = quizService.deleteQuestion(questionId, me);
        redirect.addFlashAttribute("success", "Đã xóa câu hỏi");
        return "redirect:/instructor/sections/" + quizService.getOwnedQuiz(quizId, me).getSection().getId() + "/quiz";
    }
}
