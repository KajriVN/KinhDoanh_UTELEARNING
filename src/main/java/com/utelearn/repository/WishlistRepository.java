package com.utelearn.repository;

import com.utelearn.entity.Course;
import com.utelearn.entity.User;
import com.utelearn.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserOrderByCreatedAtDesc(User user);

    Optional<Wishlist> findByUserAndCourse(User user, Course course);

    // id các khóa đã thích, để tô đỏ trái tim ở mọi thẻ khóa học
    @Query("SELECT w.course.id FROM Wishlist w WHERE w.user = :user")
    Set<Long> findCourseIdsByUser(@Param("user") User user);

    void deleteByCourse(Course course);
}
