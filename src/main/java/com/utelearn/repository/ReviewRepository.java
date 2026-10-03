package com.utelearn.repository;

import com.utelearn.entity.Course;
import com.utelearn.entity.Review;
import com.utelearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCourseOrderByCreatedAtDesc(Course course);

    boolean existsByCourseAndUser(Course course, User user);

    Optional<Review> findByCourseAndUser(Course course, User user);

    void deleteByCourse(Course course);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course = :course")
    Double averageRating(@Param("course") Course course);
}
