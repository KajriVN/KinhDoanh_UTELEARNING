package com.utelearn.repository;

import com.utelearn.entity.CartItem;
import com.utelearn.entity.Course;
import com.utelearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserOrderByAddedAtDesc(User user);

    Optional<CartItem> findByUserAndCourse(User user, Course course);

    boolean existsByUserAndCourse(User user, Course course);

    long countByUser(User user);

    void deleteByCourse(Course course);
}
