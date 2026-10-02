package com.utelearn.repository;

import com.utelearn.entity.Course;
import com.utelearn.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {

    int countByCourse(Course course);
}
