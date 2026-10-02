package com.utelearn.repository;

import com.utelearn.entity.Assignment;
import com.utelearn.entity.Submission;
import com.utelearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// TODO (hand-off): dùng cho chức năng nộp bài / chấm điểm
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);

    List<Submission> findByAssignmentOrderBySubmittedAtDesc(Assignment assignment);
}
