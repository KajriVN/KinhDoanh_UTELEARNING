package com.utelearn.repository;

import com.utelearn.entity.Lesson;
import com.utelearn.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    int countBySection(Section section);
}
