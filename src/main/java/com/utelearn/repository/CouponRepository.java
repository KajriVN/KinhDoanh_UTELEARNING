package com.utelearn.repository;

import com.utelearn.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// TODO (hand-off): dùng cho chức năng mã giảm giá
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);
}
