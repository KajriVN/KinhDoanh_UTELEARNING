package com.utelearn.repository;

import com.utelearn.entity.Course;
import com.utelearn.entity.Quiz;
import com.utelearn.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findBySection(Section section);

    List<Quiz> findBySection_Course(Course course);
}
