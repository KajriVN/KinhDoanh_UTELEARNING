package com.utelearn.repository;

import com.utelearn.entity.Course;
import com.utelearn.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByCourse(Course course);
}
