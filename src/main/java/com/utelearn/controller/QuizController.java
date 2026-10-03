package com.utelearn.controller;

import com.utelearn.entity.Quiz;
import com.utelearn.entity.QuizAttempt;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.QuizService;
import com.utelearn.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

// Học viên làm quiz cuối chương
@Controller
public class QuizController {

    private final QuizService quizService;
    private final UserService userService;

    public QuizController(QuizService quizService, UserService userService) {
        this.quizService = quizService;
        this.userService = userService;
    }

    @GetMapping("/learn/{courseId}/quizzes/{quizId}")
    public String take(@PathVariable Long courseId, @PathVariable Long quizId,
                       Principal principal, Model model, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Quiz quiz;
        try {
            quiz = quizService.getQuizForStudent(quizId, me);
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/courses/" + courseId;
        }
        model.addAttribute("quiz", quiz);
        model.addAttribute("course", quiz.getSection().getCourse());
        model.addAttribute("history", quizService.history(quiz, me));
        return "quiz/take";
    }

    // Nộp bài: các ô chọn đặt tên q_<idCâuHỏi> = 1..4
    @PostMapping("/learn/{courseId}/quizzes/{quizId}/submit")
    public String submit(@PathVariable Long courseId, @PathVariable Long quizId,
                         @RequestParam Map<String, String> params,
                         Principal principal, RedirectAttributes redirect) {
        User me = userService.findByEmail(principal.getName());
        Map<Long, Integer> answers = new HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey().startsWith("q_")) {
                try {
                    answers.put(Long.parseLong(e.getKey().substring(2)), Integer.parseInt(e.getValue()));
                } catch (NumberFormatException ex) {
                    // bỏ qua giá trị rác
                }
            }
        }
        try {
            QuizAttempt attempt = quizService.submit(me, quizId, answers);
            return "redirect:/learn/" + courseId + "/quizzes/" + quizId + "/attempts/" + attempt.getId();
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/courses/" + courseId;
        }
    }

    // Xem kết quả: đáp án đã chọn + đáp án đúng
    @GetMapping("/learn/{courseId}/quizzes/{quizId}/attempts/{attemptId}")
    public String result(@PathVariable Long courseId, @PathVariable Long quizId, @PathVariable Long attemptId,
                         Principal principal, Model model) {
        User me = userService.findByEmail(principal.getName());
        QuizAttempt attempt = quizService.getAttempt(attemptId, me);
        model.addAttribute("attempt", attempt);
        model.addAttribute("quiz", attempt.getQuiz());
        model.addAttribute("course", attempt.getQuiz().getSection().getCourse());
        model.addAttribute("chosen", attempt.getAnswerMap());
        return "quiz/result";
    }
}
