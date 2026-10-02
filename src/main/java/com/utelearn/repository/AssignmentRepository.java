package com.utelearn.repository;

import com.utelearn.entity.Assignment;
import com.utelearn.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// TODO (hand-off): dùng cho chức năng bài tập
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByCourseOrderByCreatedAtDesc(Course course);
}
