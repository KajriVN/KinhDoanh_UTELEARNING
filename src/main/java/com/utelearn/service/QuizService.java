package com.utelearn.service;

import com.utelearn.entity.Course;
import com.utelearn.entity.Quiz;
import com.utelearn.entity.QuizAttempt;
import com.utelearn.entity.QuizQuestion;
import com.utelearn.entity.Section;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.QuizAttemptRepository;
import com.utelearn.repository.QuizQuestionRepository;
import com.utelearn.repository.QuizRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Quiz trắc nghiệm cuối chương: giảng viên soạn câu hỏi, học viên làm bài (làm lại được)
@Service
public class QuizService {

    private static final int MAX_QUESTIONS = 50;

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final CurriculumService curriculumService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public QuizService(QuizRepository quizRepository, QuizQuestionRepository questionRepository,
                       QuizAttemptRepository attemptRepository, CurriculumService curriculumService,
                       CourseService courseService, EnrollmentService enrollmentService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.curriculumService = curriculumService;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    // ================== GIẢNG VIÊN ==================

    public Quiz findBySection(Section section) {
        return quizRepository.findBySection(section).orElse(null);
    }

    public Quiz getOwnedQuiz(Long quizId, User user) {
        Quiz quiz = findById(quizId);
        if (!courseService.canManage(quiz.getSection().getCourse(), user)) {
            throw new AccessDeniedException("Bạn không phải giảng viên của khóa học này");
        }
        return quiz;
    }

    // tạo mới hoặc cập nhật tên / điểm đạt của quiz cuối chương
    @Transactional
    public Quiz saveQuiz(Long sectionId, String title, int passPercent, User user) {
        Section section = curriculumService.getOwnedSection(sectionId, user);
        if (title == null || title.isBlank()) {
            throw new BusinessException("Tên bài kiểm tra không được để trống");
        }
        if (title.length() > 200) {
            throw new BusinessException("Tên bài kiểm tra tối đa 200 ký tự");
        }
        if (passPercent < 1 || passPercent > 100) {
            throw new BusinessException("Điểm đạt từ 1% đến 100%");
        }
        Quiz quiz = quizRepository.findBySection(section).orElseGet(() -> {
            Quiz q = new Quiz();
            q.setSection(section);
            return q;
        });
        quiz.setTitle(title.trim());
        quiz.setPassPercent(passPercent);
        return quizRepository.save(quiz);
    }

    @Transactional
    public Long deleteQuiz(Long quizId, User user) {
        Quiz quiz = getOwnedQuiz(quizId, user);
        Long sectionId = quiz.getSection().getId();
        attemptRepository.deleteByQuiz(quiz);
        quizRepository.delete(quiz);
        return sectionId;
    }

    // questionId = null -> thêm câu mới, ngược lại -> sửa câu đó
    @Transactional
    public QuizQuestion saveQuestion(Long quizId, Long questionId, String content, String a, String b, String c,
                                     String d, int correct, User user) {
        Quiz quiz = getOwnedQuiz(quizId, user);
        if (isBlank(content) || isBlank(a) || isBlank(b) || isBlank(c) || isBlank(d)) {
            throw new BusinessException("Vui lòng nhập đủ câu hỏi và cả 4 đáp án");
        }
        if (correct < 1 || correct > 4) {
            throw new BusinessException("Vui lòng chọn đáp án đúng");
        }
        if (a.length() > 500 || b.length() > 500 || c.length() > 500 || d.length() > 500) {
            throw new BusinessException("Mỗi đáp án tối đa 500 ký tự");
        }

        QuizQuestion question;
        if (questionId == null) {
            if (quiz.getQuestions().size() >= MAX_QUESTIONS) {
                throw new BusinessException("Mỗi bài kiểm tra tối đa " + MAX_QUESTIONS + " câu");
            }
            question = new QuizQuestion();
            question.setQuiz(quiz);
            question.setPosition(quiz.getQuestions().size() + 1);
        } else {
            question = quiz.getQuestions().stream().filter(q -> q.getId().equals(questionId)).findFirst()
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        }
        question.setContent(content.trim());
        question.setOptionA(a.trim());
        question.setOptionB(b.trim());
        question.setOptionC(c.trim());
        question.setOptionD(d.trim());
        question.setCorrectOption(correct);
        return questionRepository.save(question);
    }

    @Transactional
    public Long deleteQuestion(Long questionId, User user) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        Quiz quiz = getOwnedQuiz(question.getQuiz().getId(), user);
        quiz.getQuestions().remove(question);
        // đánh số lại cho liên tục
        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            quiz.getQuestions().get(i).setPosition(i + 1);
        }
        quizRepository.save(quiz);
        return quiz.getId();
    }

    public List<QuizAttempt> recentAttempts(Quiz quiz) {
        return attemptRepository.findTop30ByQuizOrderByTakenAtDesc(quiz);
    }

    // ================== HỌC VIÊN ==================

    public Quiz findById(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
    }

    // các quiz (đã có ít nhất 1 câu) của khóa, key = id chương -> hiện ở sidebar trang học
    public Map<Long, Quiz> quizzesOfCourse(Course course) {
        Map<Long, Quiz> map = new HashMap<>();
        for (Quiz q : quizRepository.findBySection_Course(course)) {
            if (!q.getQuestions().isEmpty()) {
                map.put(q.getSection().getId(), q);
            }
        }
        return map;
    }

    // học viên đã ghi danh mới được làm bài
    public Quiz getQuizForStudent(Long quizId, User user) {
        Quiz quiz = findById(quizId);
        if (quiz.getQuestions().isEmpty()) {
            throw new NotFoundException("Bài kiểm tra chưa có câu hỏi");
        }
        if (!enrollmentService.isEnrolled(user, quiz.getSection().getCourse())) {
            throw new BusinessException("Bạn cần đăng ký khóa học để làm bài kiểm tra");
        }
        return quiz;
    }

    public List<QuizAttempt> history(Quiz quiz, User user) {
        return attemptRepository.findByQuizAndUserOrderByTakenAtDesc(quiz, user);
    }

    public QuizAttempt getAttempt(Long attemptId, User user) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt làm bài"));
        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Không tìm thấy lượt làm bài");
        }
        return attempt;
    }

    // chấm bài: answers = idCâuHỏi -> đáp án chọn (1..4). Câu bỏ trống tính là sai.
    @Transactional
    public QuizAttempt submit(User user, Long quizId, Map<Long, Integer> answers) {
        Quiz quiz = getQuizForStudent(quizId, user);
        int correct = 0;
        StringBuilder saved = new StringBuilder();
        for (QuizQuestion q : quiz.getQuestions()) {
            Integer chosen = answers.get(q.getId());
            if (chosen == null || chosen < 1 || chosen > 4) {
                continue;
            }
            if (chosen == q.getCorrectOption()) {
                correct++;
            }
            if (saved.length() > 0) {
                saved.append(',');
            }
            saved.append(q.getId()).append(':').append(chosen);
        }
        int total = quiz.getQuestions().size();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setUser(user);
        attempt.setScore(correct);
        attempt.setTotal(total);
        attempt.setPassed(correct * 100 >= quiz.getPassPercent() * total);
        attempt.setAnswers(saved.toString());
        return attemptRepository.save(attempt);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
