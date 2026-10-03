package com.utelearn.repository;

import com.utelearn.entity.Course;
import com.utelearn.entity.OrderItem;
import com.utelearn.entity.OrderStatus;
import com.utelearn.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByCourse(Course course);

    // top khóa bán chạy: [tên khóa, số lượt bán, tổng tiền]
    @Query("SELECT i.course.title, COUNT(i), SUM(i.price) FROM OrderItem i " +
            "WHERE i.order.status = :status " +
            "GROUP BY i.course.id, i.course.title ORDER BY COUNT(i) DESC, SUM(i.price) DESC")
    List<Object[]> topCourses(@Param("status") OrderStatus status, Pageable pageable);

    // doanh thu của 1 giảng viên theo tháng: [năm, tháng, tổng tiền, số lượt bán]
    @Query("SELECT YEAR(i.order.paidAt), MONTH(i.order.paidAt), SUM(i.price), COUNT(i) FROM OrderItem i " +
            "WHERE i.order.status = :status AND i.course.instructor = :instructor " +
            "GROUP BY YEAR(i.order.paidAt), MONTH(i.order.paidAt)")
    List<Object[]> instructorRevenueByMonth(@Param("status") OrderStatus status, @Param("instructor") User instructor);

    // doanh thu của 1 giảng viên theo khóa: [id khóa, tên khóa, số lượt bán, tổng tiền]
    @Query("SELECT i.course.id, i.course.title, COUNT(i), SUM(i.price) FROM OrderItem i " +
            "WHERE i.order.status = :status AND i.course.instructor = :instructor " +
            "GROUP BY i.course.id, i.course.title ORDER BY SUM(i.price) DESC")
    List<Object[]> instructorRevenueByCourse(@Param("status") OrderStatus status, @Param("instructor") User instructor);
}
