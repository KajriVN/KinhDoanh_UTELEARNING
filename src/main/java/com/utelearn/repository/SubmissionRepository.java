package com.utelearn.repository;

import com.utelearn.entity.Assignment;
import com.utelearn.entity.Course;
import com.utelearn.entity.Submission;
import com.utelearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);

    List<Submission> findByAssignmentOrderBySubmittedAtDesc(Assignment assignment);

    // bài nộp của 1 học viên trong 1 khóa (trang bài tập phía học viên)
    List<Submission> findByStudentAndAssignment_Course(User student, Course course);

    long countByAssignment(Assignment assignment);

    // số bài nộp chưa chấm của 1 khóa
    long countByAssignment_CourseAndScoreIsNull(Course course);

    void deleteByAssignment(Assignment assignment);

    void deleteByAssignment_Course(Course course);
}
