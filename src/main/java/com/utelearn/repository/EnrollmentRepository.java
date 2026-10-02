package com.utelearn.repository;

import com.utelearn.entity.Course;
import com.utelearn.entity.Enrollment;
import com.utelearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserAndCourse(User user, Course course);

    boolean existsByUserAndCourse(User user, Course course);

    List<Enrollment> findByUserOrderByEnrolledAtDesc(User user);

    List<Enrollment> findByCourseOrderByEnrolledAtDesc(Course course);

    long countByCourse(Course course);
}
