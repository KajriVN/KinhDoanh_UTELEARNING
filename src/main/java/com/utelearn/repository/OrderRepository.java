package com.utelearn.repository;

import com.utelearn.entity.Coupon;
import com.utelearn.entity.Order;
import com.utelearn.entity.OrderStatus;
import com.utelearn.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    long countByStatus(OrderStatus status);

    boolean existsByCoupon(Coupon coupon);

    // Admin: lọc đơn theo trạng thái, khoảng ngày tạo, từ khóa (mã đơn / email / tên)
    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) " +
            "AND (:fromTime IS NULL OR o.createdAt >= :fromTime) " +
            "AND (:toTime IS NULL OR o.createdAt < :toTime) " +
            "AND (:keyword IS NULL OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "  OR LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Order> search(@Param("status") OrderStatus status,
                       @Param("fromTime") LocalDateTime fromTime,
                       @Param("toTime") LocalDateTime toTime,
                       @Param("keyword") String keyword,
                       Pageable pageable);

    // doanh thu theo tháng (đơn PAID từ mốc fromTime): [năm, tháng, tổng tiền]
    @Query("SELECT YEAR(o.paidAt), MONTH(o.paidAt), SUM(o.finalAmount) FROM Order o " +
            "WHERE o.status = :status AND o.paidAt >= :fromTime " +
            "GROUP BY YEAR(o.paidAt), MONTH(o.paidAt)")
    List<Object[]> revenueByMonth(@Param("status") OrderStatus status, @Param("fromTime") LocalDateTime fromTime);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumFinalAmountByStatus(@Param("status") OrderStatus status);
}
