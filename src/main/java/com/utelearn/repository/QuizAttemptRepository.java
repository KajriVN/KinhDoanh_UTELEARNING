package com.utelearn.repository;

import com.utelearn.entity.Course;
import com.utelearn.entity.Quiz;
import com.utelearn.entity.QuizAttempt;
import com.utelearn.entity.Section;
import com.utelearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByQuizAndUserOrderByTakenAtDesc(Quiz quiz, User user);

    List<QuizAttempt> findTop30ByQuizOrderByTakenAtDesc(Quiz quiz);

    void deleteByQuiz(Quiz quiz);

    void deleteByQuiz_Section_Course(Course course);

    void deleteByQuiz_Section(Section section);
}
