package com.utelearn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Mã giảm giá áp dụng lúc thanh toán.
// TODO (hand-off): chưa làm trang quản lý coupon + áp mã ở giỏ hàng.
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    // giảm theo % (1 - 100)
    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    // số lần dùng tối đa, 0 = không giới hạn
    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private boolean active = true;
}
