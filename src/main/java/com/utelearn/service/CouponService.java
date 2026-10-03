package com.utelearn.service;

import com.utelearn.entity.Coupon;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.CouponRepository;
import com.utelearn.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

// Mã giảm giá: admin quản lý, học viên nhập ở giỏ hàng
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    public CouponService(CouponRepository couponRepository, OrderRepository orderRepository) {
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
    }

    public List<Coupon> findAll() {
        return couponRepository.findAllByOrderByIdDesc();
    }

    public Coupon findById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã giảm giá"));
    }

    // Kiểm tra mã dùng được không: tồn tại, đang bật, chưa hết hạn, còn lượt dùng
    public Coupon validate(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Vui lòng nhập mã giảm giá");
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("Mã giảm giá không tồn tại"));
        if (!coupon.isActive()) {
            throw new BusinessException("Mã giảm giá đã bị tắt");
        }
        if (coupon.isExpired()) {
            throw new BusinessException("Mã giảm giá đã hết hạn");
        }
        if (coupon.isOutOfUses()) {
            throw new BusinessException("Mã giảm giá đã hết lượt sử dụng");
        }
        return coupon;
    }

    // giảm theo %, làm tròn xuống đồng
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(coupon.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
    }

    // id = null -> thêm mới, ngược lại -> cập nhật
    @Transactional
    public void save(Long id, String code, int discountPercent, int maxUses, LocalDateTime expiredAt, boolean active) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Mã giảm giá không được để trống");
        }
        code = code.trim().toUpperCase();
        if (!code.matches("[A-Z0-9_-]{3,50}")) {
            throw new BusinessException("Mã chỉ gồm chữ, số, gạch ngang/dưới, dài 3-50 ký tự");
        }
        if (discountPercent < 1 || discountPercent > 100) {
            throw new BusinessException("Phần trăm giảm từ 1 đến 100");
        }
        if (maxUses < 0) {
            throw new BusinessException("Số lượt dùng tối đa không được âm (0 = không giới hạn)");
        }

        Coupon coupon;
        if (id == null) {
            if (couponRepository.existsByCodeIgnoreCase(code)) {
                throw new BusinessException("Mã \"" + code + "\" đã tồn tại");
            }
            coupon = new Coupon();
        } else {
            if (couponRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new BusinessException("Mã \"" + code + "\" đã tồn tại");
            }
            coupon = findById(id);
        }
        coupon.setCode(code);
        coupon.setDiscountPercent(discountPercent);
        coupon.setMaxUses(maxUses);
        coupon.setExpiredAt(expiredAt);
        coupon.setActive(active);
        couponRepository.save(coupon);
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = findById(id);
        if (orderRepository.existsByCoupon(coupon)) {
            throw new BusinessException("Mã đã được dùng trong đơn hàng, hãy tắt mã thay vì xóa");
        }
        couponRepository.delete(coupon);
    }
}
